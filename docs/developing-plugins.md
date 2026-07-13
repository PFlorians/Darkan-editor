# Developing Plugins

This guide covers creating a new plugin for the Darkan Editor.

## Quick Start

1. Create a directory under `plugins/` (e.g., `plugins/my-plugin/`)
2. Add a `build.gradle` file
3. Implement the `IPlugin` interface with a `@PluginDescriptor` annotation
4. Create an FXML layout and controller
5. Build — the JAR lands in `plugins-runtime/` and loads at next startup

## Project Layout

```
plugins/my-plugin/
├── build.gradle
└── src/main/java/darkan/editor/plugin/
    ├── Plugin.java          @PluginDescriptor + IPlugin
    ├── Controller.kt        FXML controller
    └── resources/
        ├── scene.fxml       Plugin UI layout
        ├── css/style.css    Plugin styles
        └── icons/icon.png   Plugin icon
```

## build.gradle

```groovy
plugins {
    id 'java'
    id 'org.openjfx.javafxplugin' version '0.1.0'
}

group = 'darkan-editor.plugins'
version = '1.0.0'

javafx {
    version = '21'
    modules = ['javafx.controls', 'javafx.fxml']
}

dependencies {
    implementation(project(':plugin'))
    implementation(project(':fs'))
    implementation(project(':gui'))
    implementation(project(':io'))
    implementation(project(':shared'))
}

tasks.jar {
    destinationDirectory = rootProject.layout.projectDirectory.dir("plugins-runtime")
}
```

The `settings.gradle` in the project root automatically discovers plugin directories, so no manual registration is needed.

## Plugin Class

```java
package darkan.editor.plugin;

@PluginDescriptor(
    name = "My Plugin",
    authors = {"YourName"},
    version = "1.0.0"
)
public class Plugin implements IPlugin {

    @Override
    public String applicationIcon() {
        return "icons/icon.png";
    }

    @Override
    public String fxml() {
        return "scene.fxml";
    }

    @Override
    public String[] stylesheets() {
        return new String[]{"css/style.css"};
    }
}
```

## Controller

Extend `BaseController` from the gui module. Override `onPopulate()` (called when cache is loaded) and `onClear()` (called when cache is closed).

```kotlin
package darkan.editor.plugin

import darkan.editor.gui.App
import darkan.editor.gui.controller.BaseController
import javafx.fxml.FXML
import java.net.URL
import java.util.*

class Controller : BaseController() {

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // Set up UI bindings
    }

    override fun onPopulate() {
        val cache = App.cache ?: return
        // Load data from cache
    }

    override fun onClear() {
        // Clear UI state
    }

    @FXML
    private fun goBack() {
        switchScene("StoreScene")
    }
}
```

## Definition Plugins (ConfigExtension)

For plugins that edit game definitions (items, NPCs, objects, etc.), extend `ConfigExtension` instead of implementing `IPlugin` directly. This gives you the full decode/encode/save lifecycle for free.

```java
@PluginDescriptor(name = "My Definition Plugin", authors = {"YourName"}, version = "1.0.0")
public class Plugin extends ConfigExtension implements IPlugin {

    // --- IPlugin methods ---
    @Override public String applicationIcon() { return "icons/icon.png"; }
    @Override public String fxml() { return "scene.fxml"; }
    @Override public String[] stylesheets() { return new String[]{"css/style.css"}; }

    // --- ConfigExtension methods ---
    @Override public String getFileName() { return "mydef"; }
    @Override public int getModernIndexId() { return 18; }
    @Override public int getModernBitShift() { return 7; }

    @Override
    protected void decode(int currentIndex, RSBuffer buffer) {
        // Read fields from binary data
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) break;
            // Handle opcodes...
        }
    }

    @Override
    protected void encode(RSBuffer buffer) {
        // Write fields back to binary
        // End with: buffer.writeByte(0);
    }

    // Declare fields — these get auto-mapped to the UI table via reflection
    private String name;
    private int someValue = -1;
}
```

Key methods to override:

| Method | Purpose | Default |
|--------|---------|---------|
| `getFileName()` | Base name for legacy .dat/.idx files | Required |
| `getModernIndexId()` | Cache index for modern format | -1 (disabled) |
| `getModernBitShift()` | Bit shift to split archive/file IDs | 0 |
| `decode(index, buffer)` | Parse binary data into fields | Required |
| `encode(buffer)` | Serialize fields back to binary | Required |
| `getStoreId()` | Legacy file store ID | ARCHIVE_FILE_STORE |
| `getFileId()` | Legacy archive ID | CONFIG_ARCHIVE |
| `useMetaFile()` | Whether a separate .idx file exists | true |

## Event Bus

Plugins can communicate via the Guava `EventBus`:

```kotlin
// Listen for events
@Subscribe
fun onCacheLoaded(event: LoadCacheEvent) {
    // React to cache being loaded
}

// Post events
PluginManager.post(MyCustomEvent(data))
```

Plugins are automatically registered with the event bus when loaded.

## Accessing the Cache

```kotlin
// Get the active cache (null if none loaded)
val cache = App.cache ?: return

// Read a file
val data: ByteBuffer? = cache.readFile(indexId, archiveId, fileId)

// Write a file
cache.writeFile(indexId, archiveId, fileId, byteArray)

// Check format
if (cache.format == CacheFormat.MODERN) { ... }

// Navigate indices
val index = cache.getIndex(indexId)
val archive = index.getArchive(archiveId)
val fileIds = archive.fileIds
```

## Tips

- All plugin fields are mapped to the UI table via reflection. Use descriptive field names — they become the column labels.
- The `RSBuffer` class provides all RuneScape-specific read/write operations (unsigned bytes, shorts, tri-bytes, strings, big-smart integers).
- Test your decode/encode round-trip: decode a definition, encode it, and verify the output matches the original bytes.
- Plugins are loaded in isolated classloaders, so they won't conflict with each other's dependencies.
