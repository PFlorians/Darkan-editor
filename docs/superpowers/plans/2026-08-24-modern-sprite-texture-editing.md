# Modern Sprite & Texture Editing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable viewing, exporting, importing, and replacing sprites and textures in modern (post-317 OSRS) caches using RuneLite for decoding and OpenRS2 for encoding.

**Architecture:** RuneLite's `cache` module decodes modern sprites (Index 8) and textures (Index 9) from raw cache bytes into usable image data. OpenRS2's `cache-550` module provides the sprite encoder to write images back to cache binary format. A codec layer in the `fs` module wraps both libraries, exposing `BufferedImage`-based APIs to plugins. The existing sprite-plugin and texture-plugin gain modern cache support with export-to-PNG, import-from-PNG, and direct replacement workflows.

**Tech Stack:** Java 21, Kotlin, JavaFX 21, RuneLite cache 1.12.36, OpenRS2 cache-550 0.1.0, Displee rs-cache-library 8.1.0

**Spec:** This plan is derived from the brainstorming session (no separate spec file — bounded/hybrid design agreed in conversation).

## Global Constraints

- Java 21 toolchain, Kotlin 2.4.0
- Gradle 9.5.0
- RuneLite cache: `net.runelite:cache:1.12.36` (Maven Central)
- OpenRS2 cache-550: `org.openrs2:openrs2-cache-550:0.1.0` (repo: `https://repo.openrs2.org/repository/openrs2`)
- Displee rs-cache-library 8.1.0 remains the sole cache I/O layer
- All sprite decode/encode logic lives in `fs` module codec package — plugins never import RuneLite/OpenRS2 directly
- Modern sprites: Index 8, one archive per sprite group, each file is a frame
- Modern textures: Index 9, archive 0, each file is a texture definition (metadata referencing sprite IDs)

---

### Task 1: Add RuneLite and OpenRS2 Dependencies

**Files:**
- Modify: `fs/build.gradle`
- Modify: `settings.gradle` (add OpenRS2 repository at root level)
- Modify: `build.gradle` (add OpenRS2 repository to allprojects)

**Interfaces:**
- Consumes: Nothing
- Produces: RuneLite `SpriteLoader`, `TextureLoader` classes available in `fs` module classpath. OpenRS2 `Sprite` class available for encoding.

- [ ] **Step 1: Add the OpenRS2 Maven repository to the root build.gradle**

In `build.gradle`, add the OpenRS2 repository inside the `allprojects.repositories` block:

```groovy
allprojects {
  group = 'darkan-editor'
  version = '3.1.0'

  repositories {
    mavenCentral()
    mavenLocal()
    maven { url 'https://repo.openrs2.org/repository/openrs2' }
  }
}
```

- [ ] **Step 2: Add RuneLite cache and OpenRS2 cache-550 dependencies to fs/build.gradle**

```groovy
dependencies {
  implementation(project(":io"))
  implementation(project(":util"))
  implementation 'com.displee:rs-cache-library:8.1.0'
  implementation 'net.runelite:cache:1.12.36'
  implementation 'org.openrs2:openrs2-cache-550:0.1.0'

  testImplementation 'org.junit.jupiter:junit-jupiter:5.11.4'
  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

- [ ] **Step 3: Run Gradle to verify dependencies resolve**

Run: `./gradlew :fs:dependencies --configuration runtimeClasspath`
Expected: Both `net.runelite:cache:1.12.36` and `org.openrs2:openrs2-cache-550:0.1.0` appear in the tree without resolution errors.

- [ ] **Step 4: Verify the project compiles**

Run: `./gradlew :fs:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add build.gradle fs/build.gradle
git commit -m "build: add RuneLite cache and OpenRS2 cache-550 dependencies"
```

---

### Task 2: Create Modern Sprite Codec (Decode + Encode)

**Files:**
- Create: `fs/src/main/java/darkan/editor/fs/codec/ModernSpriteCodec.java`

**Interfaces:**
- Consumes: RuneLite `SpriteLoader`, `SpriteDefinition`. OpenRS2 `Sprite` class. `CacheSystem.readFile()`, `CacheSystem.writeFile()`.
- Produces:
  - `ModernSpriteCodec.decode(byte[] data): List<ModernSpriteFrame>` — decodes a sprite archive entry into frames
  - `ModernSpriteCodec.encode(List<BufferedImage> frames, int maxWidth, int maxHeight): byte[]` — encodes frames back to cache binary format
  - `ModernSpriteFrame` — record holding `id`, `frame`, `width`, `height`, `offsetX`, `offsetY`, `image` (BufferedImage)

- [ ] **Step 1: Write a failing test for sprite decoding**

Create `fs/src/test/java/darkan/editor/fs/codec/ModernSpriteCodecTest.java`:

```java
package darkan.editor.fs.codec;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ModernSpriteCodecTest {

    @Test
    void decodeAndEncodeRoundTrip() {
        // Create a simple 4x4 image with known colors
        BufferedImage original = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                original.setRGB(x, y, 0xFFFF0000); // solid red
            }
        }

        // Encode the image
        byte[] encoded = ModernSpriteCodec.encode(List.of(original), 4, 4);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        // Decode it back
        List<ModernSpriteFrame> frames = ModernSpriteCodec.decode(encoded);
        assertEquals(1, frames.size());

        ModernSpriteFrame frame = frames.get(0);
        assertEquals(4, frame.width());
        assertEquals(4, frame.height());

        // Verify pixel colors match (ignoring alpha since RS format may normalize it)
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                int pixel = frame.image().getRGB(x, y);
                assertEquals(0xFFFF0000, pixel, "Pixel mismatch at " + x + "," + y);
            }
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :fs:test --tests "darkan.editor.fs.codec.ModernSpriteCodecTest" -i`
Expected: FAIL — class `ModernSpriteCodec` does not exist.

- [ ] **Step 3: Create the ModernSpriteFrame record**

Create `fs/src/main/java/darkan/editor/fs/codec/ModernSpriteFrame.java`:

```java
package darkan.editor.fs.codec;

import java.awt.image.BufferedImage;

public record ModernSpriteFrame(
    int id,
    int frame,
    int width,
    int height,
    int offsetX,
    int offsetY,
    int maxWidth,
    int maxHeight,
    BufferedImage image
) {}
```

- [ ] **Step 4: Implement ModernSpriteCodec**

Create `fs/src/main/java/darkan/editor/fs/codec/ModernSpriteCodec.java`:

```java
package darkan.editor.fs.codec;

import net.runelite.cache.definitions.SpriteDefinition;
import net.runelite.cache.definitions.loaders.SpriteLoader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.openrs2.cache550.sprite.Sprite;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;

public final class ModernSpriteCodec {

    private ModernSpriteCodec() {}

    public static List<ModernSpriteFrame> decode(byte[] data) {
        SpriteLoader loader = new SpriteLoader();
        SpriteDefinition[] defs = loader.load(0, data);
        List<ModernSpriteFrame> frames = new ArrayList<>();

        if (defs == null) {
            return frames;
        }

        for (SpriteDefinition def : defs) {
            int w = def.getWidth();
            int h = def.getHeight();
            if (w <= 0 || h <= 0) continue;

            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            int[] destPixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            int[] srcPixels = def.getPixels();
            System.arraycopy(srcPixels, 0, destPixels, 0, srcPixels.length);

            frames.add(new ModernSpriteFrame(
                def.getId(),
                def.getFrame(),
                w, h,
                def.getOffsetX(),
                def.getOffsetY(),
                def.getMaxWidth(),
                def.getMaxHeight(),
                image
            ));
        }

        return frames;
    }

    public static byte[] encode(List<BufferedImage> frames, int maxWidth, int maxHeight) {
        ByteBuf buf = Unpooled.buffer();
        try {
            Sprite sprite = Sprite.fromImages(frames, maxWidth, maxHeight);
            sprite.write(buf);
            byte[] result = new byte[buf.readableBytes()];
            buf.readBytes(result);
            return result;
        } finally {
            buf.release();
        }
    }
}
```

**Note:** The exact OpenRS2 import paths and method signatures may differ from what's shown here. The implementer should check the actual `org.openrs2` package structure after the dependency resolves. The key class is `Sprite` in the `cache-550` module with `fromImages()`/`fromImage()` and `write(ByteBuf)` methods. If the package path differs, adjust the import accordingly.

- [ ] **Step 5: Run the test**

Run: `./gradlew :fs:test --tests "darkan.editor.fs.codec.ModernSpriteCodecTest" -i`
Expected: PASS. If it fails due to OpenRS2 API differences, adjust the `encode()` method to match the actual API (the `Sprite` class may need individual frame construction rather than a list factory).

- [ ] **Step 6: Commit**

```bash
git add fs/src/main/java/darkan/editor/fs/codec/ fs/src/test/java/darkan/editor/fs/codec/
git commit -m "feat: add ModernSpriteCodec with RuneLite decode and OpenRS2 encode"
```

---

### Task 3: Create Modern Texture Codec (Decode + Encode)

**Files:**
- Create: `fs/src/main/java/darkan/editor/fs/codec/ModernTextureCodec.java`
- Create: `fs/src/main/java/darkan/editor/fs/codec/ModernTextureDef.java`

**Interfaces:**
- Consumes: RuneLite `TextureLoader`, `TextureDefinition`. `ModernSpriteCodec` for resolving sprite references.
- Produces:
  - `ModernTextureDef` — record with `id`, `spriteIds` (int[]), `animationSpeed`, `animationDirection`
  - `ModernTextureCodec.decode(byte[] data): ModernTextureDef` — decodes texture metadata
  - `ModernTextureCodec.encode(ModernTextureDef def): byte[]` — encodes texture metadata back to binary
  - `ModernTextureCodec.renderTexture(ModernTextureDef def, CacheSystem cache): BufferedImage` — resolves sprite reference and returns the rendered texture image

- [ ] **Step 1: Write a failing test for texture decode/encode round-trip**

Create `fs/src/test/java/darkan/editor/fs/codec/ModernTextureCodecTest.java`:

```java
package darkan.editor.fs.codec;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModernTextureCodecTest {

    @Test
    void decodeAndEncodeRoundTrip() {
        // Texture definition is simple metadata: spriteId + flags
        ModernTextureDef original = new ModernTextureDef(
            42, new int[]{100}, 0, 0, false
        );

        byte[] encoded = ModernTextureCodec.encode(original);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        ModernTextureDef decoded = ModernTextureCodec.decode(42, encoded);
        assertEquals(42, decoded.id());
        assertArrayEquals(new int[]{100}, decoded.spriteIds());
        assertEquals(0, decoded.animationSpeed());
        assertEquals(0, decoded.animationDirection());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :fs:test --tests "darkan.editor.fs.codec.ModernTextureCodecTest" -i`
Expected: FAIL — classes don't exist.

- [ ] **Step 3: Create ModernTextureDef record**

Create `fs/src/main/java/darkan/editor/fs/codec/ModernTextureDef.java`:

```java
package darkan.editor.fs.codec;

public record ModernTextureDef(
    int id,
    int[] spriteIds,
    int animationSpeed,
    int animationDirection,
    boolean field1778
) {}
```

- [ ] **Step 4: Implement ModernTextureCodec**

Create `fs/src/main/java/darkan/editor/fs/codec/ModernTextureCodec.java`:

```java
package darkan.editor.fs.codec;

import net.runelite.cache.definitions.TextureDefinition;
import net.runelite.cache.definitions.loaders.TextureLoader;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public final class ModernTextureCodec {

    private ModernTextureCodec() {}

    public static ModernTextureDef decode(int id, byte[] data) {
        TextureLoader loader = new TextureLoader();
        TextureDefinition def = loader.load(id, data);

        return new ModernTextureDef(
            id,
            def.getFileIds(),
            def.getAnimationSpeed(),
            def.getAnimationDirection(),
            def.isField1778()
        );
    }

    public static byte[] encode(ModernTextureDef def) {
        // Modern texture format (rev233):
        // - 1 unsigned short: sprite file ID (first spriteId)
        // - 1 unsigned short: missing color (0)
        // - 1 unsigned byte: field1778 (boolean as 0/1)
        // - 1 unsigned byte: animation direction
        // - 1 unsigned byte: animation speed
        int size = 2 + 2 + 1 + 1 + 1;
        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.putShort((short) (def.spriteIds().length > 0 ? def.spriteIds()[0] : 0));
        buf.putShort((short) 0); // missingColor
        buf.put((byte) (def.field1778() ? 1 : 0));
        buf.put((byte) def.animationDirection());
        buf.put((byte) def.animationSpeed());
        return buf.array();
    }

    public static BufferedImage renderTexture(ModernTextureDef def, 
                                              java.util.function.Function<Integer, BufferedImage> spriteResolver) {
        if (def.spriteIds().length == 0) {
            return new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        }
        BufferedImage sprite = spriteResolver.apply(def.spriteIds()[0]);
        if (sprite == null) {
            return new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        }
        return sprite;
    }
}
```

- [ ] **Step 5: Run the test**

Run: `./gradlew :fs:test --tests "darkan.editor.fs.codec.ModernTextureCodecTest" -i`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add fs/src/main/java/darkan/editor/fs/codec/ModernTexture* fs/src/test/java/darkan/editor/fs/codec/ModernTextureCodecTest.java
git commit -m "feat: add ModernTextureCodec for texture metadata decode/encode"
```

---

### Task 4: Update Sprite Plugin for Modern Cache Support

**Files:**
- Modify: `plugins/sprite-plugin/src/main/java/darkan/editor/plugin/Plugin.kt`
- Modify: `plugins/sprite-plugin/src/main/java/darkan/editor/plugin/Controller.kt`
- Modify: `plugins/sprite-plugin/build.gradle`

**Interfaces:**
- Consumes: `ModernSpriteCodec.decode()`, `ModernSpriteCodec.encode()`, `CacheSystem`, `CacheIndex`, `CacheArchive`
- Produces: Sprite plugin supports modern caches — browse, view, export, import, replace, pack.

- [ ] **Step 1: Update Plugin.kt descriptor**

Change the plugin name from "Vanilla 317 Sprite Plugin" to "Sprite Plugin" since it now supports both formats:

```kotlin
package darkan.editor.plugin

@PluginDescriptor(name = "Sprite Plugin", authors = ["Nshusa"], version = "2.0.0")
class Plugin : IPlugin {

    override fun applicationIcon(): String {
        return "icons/icon.png"
    }

    override fun fxml(): String {
        return "scene.fxml"
    }

    override fun stylesheets(): Array<String> {
        return arrayOf("css/style.css")
    }
}
```

- [ ] **Step 2: Add modern cache loading to Controller.kt onPopulate()**

Replace the early-return block for modern caches in `Controller.kt` `onPopulate()` with logic that loads sprites from Index 8:

```kotlin
override fun onPopulate() {
    archives.clear()
    sprites.clear()

    val cache = App.cache ?: return

    if (cache.format == CacheFormat.MODERN) {
        loadModernSprites(cache)
        return
    }

    // ... existing legacy loading code stays unchanged ...
}

private fun loadModernSprites(cache: CacheSystem) {
    val index = cache.getIndex(8) ?: return // Index 8 = Sprites
    val archiveIds = index.archiveIds

    for (archiveId in archiveIds) {
        val data = cache.readFile(8, archiveId) ?: continue
        if (data.remaining() == 0) continue

        try {
            val bytes = ByteArray(data.remaining())
            data.get(bytes)
            val frames = darkan.editor.fs.codec.ModernSpriteCodec.decode(bytes)
            if (frames.isEmpty()) continue

            val spriteModels = mutableListOf<SpriteModel>()
            for (frame in frames) {
                val rsSprite = RSSprite(
                    frame.maxWidth(), frame.maxHeight(),
                    frame.offsetX(), frame.offsetY(),
                    frame.width(), frame.height(),
                    0, // format: horizontal by default for modern
                    imageToPixelArray(frame.image())
                )
                spriteModels.add(SpriteModel(frame.frame(), rsSprite))
            }

            archives.add(ImageArchiveModel(archiveId, spriteModels))
        } catch (ex: Exception) {
            System.err.println("Failed to decode sprite archive $archiveId: ${ex.message}")
        }
    }
}

private fun imageToPixelArray(image: BufferedImage): IntArray {
    val w = image.width
    val h = image.height
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val argb = image.getRGB(x, y)
            // Convert ARGB to RGB, treating transparent as 0
            pixels[x + y * w] = if ((argb ushr 24) == 0) 0 else (argb and 0xFFFFFF)
        }
    }
    return pixels
}
```

- [ ] **Step 3: Add modern export support**

The existing `exportArchive()` and `exportSprite()` methods already work via `RSSprite.toBufferedImage()` → `ImageIO.write()`, which is format-agnostic. No changes needed for export.

Verify by reading the code — both methods use `spriteModel.sprite.toBufferedImage()` which works for both legacy and modern sprites since the pixel data is already decoded into the `RSSprite` fields.

- [ ] **Step 4: Add modern pack (save) support**

Add a `packModern()` method that encodes sprites back to cache using `ModernSpriteCodec.encode()`:

```kotlin
private fun packModern() {
    val cache = App.cache ?: return

    for (archiveModel in archives) {
        if (archiveModel.sprites.isEmpty()) continue

        val images = mutableListOf<BufferedImage>()
        var maxWidth = 0
        var maxHeight = 0

        for (spriteModel in archiveModel.sprites) {
            val bimage = spriteModel.sprite.toBufferedImage()
            images.add(bimage)
            if (bimage.width > maxWidth) maxWidth = bimage.width
            if (bimage.height > maxHeight) maxHeight = bimage.height
        }

        val encoded = darkan.editor.fs.codec.ModernSpriteCodec.encode(images, maxWidth, maxHeight)
        cache.writeFile(8, archiveModel.hash, encoded)
    }

    val alert = Alert(Alert.AlertType.INFORMATION)
    alert.title = "Info"
    alert.headerText = "Success!"
    alert.showAndWait()
}
```

Modify the existing `pack()` method to dispatch based on cache format:

```kotlin
@FXML
private fun pack() {
    val cache = App.cache ?: return
    if (cache.format == CacheFormat.MODERN) {
        packModern()
        return
    }
    // ... existing legacy pack code ...
}
```

- [ ] **Step 5: Verify the plugin compiles**

Run: `./gradlew :plugins-sprite-plugin:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Manual test with the sample cache**

Run: `./gradlew run`
- Open the cache at `/home/dev/prototypes/cache`
- Open Sprite Plugin
- Verify sprites from Index 8 appear in the archive list
- Select a sprite, verify it renders in the canvas
- Export a sprite to PNG, verify the file is valid
- Replace a sprite with a different PNG, verify it updates in the UI
- Pack and reload to verify round-trip

- [ ] **Step 7: Commit**

```bash
git add plugins/sprite-plugin/
git commit -m "feat: add modern cache support to sprite plugin (view, export, import, replace)"
```

---

### Task 5: Update Texture Plugin for Modern Cache Support

**Files:**
- Modify: `plugins/texture-plugin/src/main/java/darkan/editor/plugin/Plugin.kt`
- Modify: `plugins/texture-plugin/src/main/java/darkan/editor/plugin/Controller.kt`
- Modify: `plugins/texture-plugin/build.gradle`

**Interfaces:**
- Consumes: `ModernTextureCodec.decode()`, `ModernTextureCodec.encode()`, `ModernTextureCodec.renderTexture()`, `ModernSpriteCodec.decode()`, `CacheSystem`
- Produces: Texture plugin supports modern caches — browse textures, see rendered texture image (resolved from sprite), export, replace sprite, save.

- [ ] **Step 1: Update Plugin.kt descriptor**

```kotlin
package darkan.editor.plugin

@PluginDescriptor(name = "Texture Plugin", authors = ["Nshusa"], version = "2.0.0")
class Plugin : IPlugin {

    override fun applicationIcon(): String {
        return "icons/icon.png"
    }

    override fun fxml(): String {
        return "scene.fxml"
    }

    override fun stylesheets(): Array<String> {
        return arrayOf("css/style.css")
    }
}
```

- [ ] **Step 2: Add modern cache loading to Controller.kt onPopulate()**

Replace the early-return block for modern caches with logic that loads textures from Index 9 and resolves their sprite images from Index 8:

```kotlin
override fun onPopulate() {
    items.clear()

    val cache = App.cache ?: return

    if (cache.format == CacheFormat.MODERN) {
        loadModernTextures(cache)
        return
    }

    // ... existing legacy loading code stays unchanged ...
}

private fun loadModernTextures(cache: CacheSystem) {
    val textureIndex = cache.getIndex(9) ?: return // Index 9 = Textures
    val archive = textureIndex.getArchive(0) ?: return // Textures live in archive 0
    val fileIds = archive.fileIds

    for (fileId in fileIds) {
        val data = cache.readFile(9, 0, fileId) ?: continue
        if (data.remaining() == 0) continue

        try {
            val bytes = ByteArray(data.remaining())
            data.get(bytes)
            val texDef = darkan.editor.fs.codec.ModernTextureCodec.decode(fileId, bytes)

            // Resolve the sprite image for this texture
            val image = darkan.editor.fs.codec.ModernTextureCodec.renderTexture(texDef) { spriteId ->
                resolveModernSprite(cache, spriteId)
            }

            val rsSprite = RSSprite(image)
            items.add(SpriteModel(fileId, rsSprite))
        } catch (ex: Exception) {
            System.err.println("Failed to decode texture $fileId: ${ex.message}")
        }
    }
}

private fun resolveModernSprite(cache: CacheSystem, spriteId: Int): BufferedImage? {
    val data = cache.readFile(8, spriteId) ?: return null
    if (data.remaining() == 0) return null

    val bytes = ByteArray(data.remaining())
    data.get(bytes)
    val frames = darkan.editor.fs.codec.ModernSpriteCodec.decode(bytes)
    if (frames.isEmpty()) return null
    return frames[0].image()
}
```

- [ ] **Step 3: Add modern replace support**

Replacing a texture in modern format means replacing the underlying sprite in Index 8. Modify `replaceTexture()`:

```kotlin
@FXML
private fun replaceTexture() {
    val selectedItem = listView.selectionModel.selectedItem ?: return
    val cache = App.cache ?: return

    val chooser = FileChooser()
    chooser.initialDirectory = File("./")
    chooser.title = "Select the replacement image"
    chooser.extensionFilters.add(FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"))

    val selectedFile = chooser.showOpenDialog(App.mainStage) ?: return

    try {
        var bimage = ImageIO.read(selectedFile) ?: return

        if (cache.format == CacheFormat.MODERN) {
            replaceModernTexture(selectedItem, bimage, cache)
        } else {
            replaceLegacyTexture(selectedItem, bimage)
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

private fun replaceModernTexture(selectedItem: SpriteModel, bimage: BufferedImage, cache: CacheSystem) {
    // Read the texture definition to find the sprite ID
    val texData = cache.readFile(9, 0, selectedItem.id) ?: return
    val texBytes = ByteArray(texData.remaining())
    texData.get(texBytes)
    val texDef = darkan.editor.fs.codec.ModernTextureCodec.decode(selectedItem.id, texBytes)

    if (texDef.spriteIds().isEmpty()) return
    val spriteId = texDef.spriteIds()[0]

    // Encode the new image as a sprite and write to Index 8
    val encoded = darkan.editor.fs.codec.ModernSpriteCodec.encode(
        listOf(bimage), bimage.width, bimage.height
    )
    cache.writeFile(8, spriteId, encoded)

    // Update the UI model
    val rsSprite = RSSprite(bimage)
    selectedItem.sprite = rsSprite
    updateInfo(selectedItem.id, rsSprite)
    listView.refresh()

    val alert = Alert(Alert.AlertType.INFORMATION)
    alert.title = "Info"
    alert.headerText = "Texture sprite replaced successfully!"
    alert.showAndWait()
}

private fun replaceLegacyTexture(selectedItem: SpriteModel, bimage: BufferedImage) {
    // Existing legacy replace logic (extracted from current replaceTexture())
    if (bimage.type != BufferedImage.TYPE_INT_RGB) {
        bimage = bimage.toType(BufferedImage.TYPE_INT_RGB)
    }

    val sprite = RSSprite(bimage)

    if (bimage.width < 64 || bimage.width > 128 || bimage.height < 64 || bimage.height > 128) {
        val alert = Alert(Alert.AlertType.WARNING)
        alert.headerText = "Image width/height must be between 64-128"
        alert.showAndWait()
        return
    }

    val colors = sprite.pixels.toSet()
    if (colors.size > 255) {
        val alert = Alert(Alert.AlertType.WARNING)
        alert.headerText = "Image exceeds color limit=255 colors=${colors.size}"
        alert.showAndWait()
        return
    }

    items[selectedItem.id].sprite = sprite
    updateInfo(selectedItem.id, sprite)
    listView.refresh()
}
```

- [ ] **Step 4: Add modern export support**

The existing `export()` and `exportAll()` methods already use `sprite.toBufferedImage()` → `ImageIO.write()`. These work for modern sprites too since they're stored as `RSSprite` in the model. No code changes needed.

- [ ] **Step 5: Verify the plugin compiles**

Run: `./gradlew :plugins-texture-plugin:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Manual test with the sample cache**

Run: `./gradlew run`
- Open the cache at `/home/dev/prototypes/cache`
- Open Texture Plugin
- Verify textures appear in the list (resolved from their sprite references)
- Export a texture to PNG
- Replace a texture with a new PNG, verify it updates
- Close and reopen the cache to verify persistence

- [ ] **Step 7: Commit**

```bash
git add plugins/texture-plugin/
git commit -m "feat: add modern cache support to texture plugin (view, export, replace)"
```

---

### Task 6: Update Documentation

**Files:**
- Modify: `docs/plugins.md`
- Modify: `docs/architecture.md`
- Create: `docs/library-integration.md`

**Interfaces:**
- Consumes: Completed implementation from Tasks 1-5
- Produces: Updated documentation reflecting new capabilities and library choices

- [ ] **Step 1: Create docs/library-integration.md**

```markdown
# Library Integration

## Overview

Darkan Editor uses a hybrid approach for cache data handling:

- **Displee rs-cache-library 8.1.0** — Cache I/O (reading/writing raw bytes to/from cache files)
- **RuneLite cache 1.12.36** — Decoding modern cache formats (sprites, textures, definitions)
- **OpenRS2 cache-550 0.1.0** — Encoding sprites back to cache binary format

## Why Three Libraries?

Each library excels at a different part of the pipeline:

| Library | Strength | Weakness |
|---------|----------|----------|
| Displee | Cache container I/O, index management | No format-specific decoders for modern cache |
| RuneLite | Comprehensive decoders for all modern formats | No encoders for sprites/textures |
| OpenRS2 | Sprite encoder with palette optimization | Not on Maven Central, Kotlin/Netty deps |

## Dependency Flow

```
Plugins → fs/codec layer → RuneLite (decode) + OpenRS2 (encode)
                         → Displee (cache I/O)
```

Plugins never import RuneLite or OpenRS2 directly. The `fs/codec` package wraps both libraries
and exposes `BufferedImage`-based APIs.

## Repositories

```groovy
repositories {
    mavenCentral()                                          // Displee, RuneLite
    maven { url 'https://repo.openrs2.org/repository/openrs2' }  // OpenRS2
}
```

## Key Classes

### ModernSpriteCodec (fs/codec)
- `decode(byte[] data): List<ModernSpriteFrame>` — RuneLite's SpriteLoader under the hood
- `encode(List<BufferedImage> frames, int maxWidth, int maxHeight): byte[]` — OpenRS2's Sprite encoder

### ModernTextureCodec (fs/codec)
- `decode(int id, byte[] data): ModernTextureDef` — RuneLite's TextureLoader
- `encode(ModernTextureDef def): byte[]` — Manual encoding (format is trivial: spriteId + flags)
- `renderTexture(def, spriteResolver): BufferedImage` — Resolves sprite ID to viewable image

## Modern Sprite Format (Index 8)

Binary format (read from end of file):
1. Last 2 bytes: frame count
2. Max width (2), max height (2), palette size - 1 (1)
3. Per-frame: offsetX (2), offsetY (2), width (2), height (2)
4. Palette: (size - 1) * 3 bytes RGB
5. Pixel data from offset 0: flag byte (vertical|alpha), palette indices, optional alpha bytes

## Modern Texture Format (Index 9)

Each file in archive 0 is a texture definition (rev233 format):
1. Sprite file ID (2 bytes) — references a sprite in Index 8
2. Missing color (2 bytes)
3. Field1778 flag (1 byte)
4. Animation direction (1 byte)
5. Animation speed (1 byte)

Textures are rendered by resolving their sprite reference from Index 8.

## Future: Map Support

Map regions (Index 5) will require:
- XTEA keys for decryption (available from OpenRS2 public API: `https://archive.openrs2.org/keys`)
- RuneLite's Region class for terrain/object decoding
- A new map plugin with a tile-based editor UI
```

- [ ] **Step 2: Update docs/plugins.md**

Update the Sprite Plugin and Texture Plugin entries to reflect their new capabilities:

Sprite Plugin entry:
```markdown
## Sprite Plugin

**Purpose:** View, export, import, and replace sprites in both legacy and modern caches.

**Cache location:**
- Legacy: Media archive (file store 0, archive 4)
- Modern: Index 8 (one archive per sprite group, files are frames)

**Capabilities:**
- Browse sprite archives/groups
- View individual sprites and frames
- Export single sprites or entire archives to PNG
- Import new sprites from PNG files
- Replace existing sprites
- Pack changes back to cache (both legacy and modern)

**Encoding:** Uses OpenRS2 sprite encoder for modern format (palette optimization, alpha support). Legacy uses built-in encoder.
```

Texture Plugin entry:
```markdown
## Texture Plugin

**Purpose:** View, export, and replace textures in both legacy and modern caches.

**Cache location:**
- Legacy: Texture archive (file store 0, archive 6)
- Modern: Index 9 (archive 0, each file is a texture definition referencing a sprite in Index 8)

**Capabilities:**
- Browse all textures
- View textures (modern: resolves sprite reference from Index 8)
- Export textures to PNG
- Replace texture sprites with new PNG images
- Pack changes back to cache (legacy)

**Notes:** Modern textures are metadata pointing to sprites. Replacing a texture replaces its underlying sprite in Index 8.
```

- [ ] **Step 3: Update docs/architecture.md**

Add a section about the codec layer after the "fs (File System)" module description:

```markdown
#### fs/codec (Format Codecs)

Wraps external libraries for format-specific decoding and encoding:

- `ModernSpriteCodec` — Decode (RuneLite) and encode (OpenRS2) modern sprites
- `ModernSpriteFrame` — Data record for a decoded sprite frame
- `ModernTextureCodec` — Decode and encode modern texture definitions
- `ModernTextureDef` — Data record for texture metadata

Plugins use these codecs via `BufferedImage` — they never interact with RuneLite or OpenRS2 directly.
```

- [ ] **Step 4: Commit**

```bash
git add docs/
git commit -m "docs: add library integration guide, update plugin and architecture docs"
```

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| OpenRS2 `Sprite` API differs from expected | Task 2 implementation breaks | Check actual API after dependency resolves; adapt `encode()` method. Worst case: write a manual encoder using the format spec from RuneLite's decoder. |
| RuneLite `SpriteLoader.load()` returns null for some archives | Some sprites don't render | Wrap in try/catch, skip failed archives, log warning |
| Netty `ByteBuf` conflicts with existing code | Runtime errors | OpenRS2 uses Netty internally; we only touch ByteBuf in the codec layer, then return `byte[]` |
| OpenRS2 repository is unavailable | Build fails | Pin a local copy of the jar as a fallback; or fall back to Approach B/C |
| Alpha channel handling differences | Colors look wrong | RuneLite decodes to ARGB; ensure BufferedImage uses TYPE_INT_ARGB in codec layer, convert to TYPE_INT_RGB only when needed for legacy RSSprite |
