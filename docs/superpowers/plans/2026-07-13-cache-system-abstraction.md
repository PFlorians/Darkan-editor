# Cache System Abstraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce a `CacheSystem` interface abstraction in the `fs` module that supports both legacy v317 and modern (post-317) RS caches via Displee's rs-cache-library, with auto-detection and a factory pattern.

**Architecture:** A `CacheSystem` interface models the modern cache hierarchy (index → archive → file). Two implementations: `LegacyCacheSystem` wraps existing `RSFileSystem`/`RSFileStore`/`RSArchive`; `ModernCacheSystem` wraps Displee's `CacheLibrary`. A `CacheSystemFactory` detects the format from a path and returns the correct implementation. Consumers (`App`, plugins) use the interface.

**Tech Stack:** Java 21, Kotlin, Gradle 9.5, Displee rs-cache-library 7.3.0, JUnit 5, JavaFX 21

## Global Constraints

- Java 21 toolchain
- Gradle 9.5.0
- Displee rs-cache-library version: `7.3.0` (from Maven Central `com.displee:rs-cache-library:7.3.0`)
- All new interfaces/classes in package `darkan.editor.fs`
- Existing `RSFileSystem`, `RSFileStore`, `RSArchive` remain as internal implementation details — no deletion
- All existing plugins must compile and work after migration
- Test cache path: `/home/dev/prototypes/cache` (modern format with `main_file_cache.dat2`)

## File Map

| Action | Path | Responsibility |
|--------|------|---------------|
| Create | `fs/src/main/java/darkan/editor/fs/CacheConstants.java` | File name constants |
| Create | `fs/src/main/java/darkan/editor/fs/CacheFormat.java` | Enum for cache format |
| Create | `fs/src/main/java/darkan/editor/fs/CacheSystem.java` | Main interface |
| Create | `fs/src/main/java/darkan/editor/fs/CacheIndex.java` | Index interface |
| Create | `fs/src/main/java/darkan/editor/fs/CacheArchive.java` | Archive interface |
| Create | `fs/src/main/java/darkan/editor/fs/CacheSystemFactory.java` | Factory with auto-detection |
| Create | `fs/src/main/java/darkan/editor/fs/LegacyCacheSystem.java` | v317 adapter |
| Create | `fs/src/main/java/darkan/editor/fs/LegacyCacheIndex.java` | v317 index adapter |
| Create | `fs/src/main/java/darkan/editor/fs/LegacyCacheArchive.java` | v317 archive adapter |
| Create | `fs/src/main/java/darkan/editor/fs/ModernCacheSystem.java` | Displee wrapper |
| Create | `fs/src/main/java/darkan/editor/fs/ModernCacheIndex.java` | Displee index wrapper |
| Create | `fs/src/main/java/darkan/editor/fs/ModernCacheArchive.java` | Displee archive wrapper |
| Modify | `fs/build.gradle` | Add Displee dependency + JUnit 5 |
| Modify | `gui/src/main/kotlin/darkan/editor/gui/App.kt` | Replace `RSFileSystem` singleton with `CacheSystem` |
| Modify | `gui/src/main/kotlin/darkan/editor/gui/event/LoadCacheEvent.kt` | Use `CacheSystem` type |
| Modify | `gui/src/main/kotlin/darkan/editor/gui/controller/BaseController.kt` | Use new `App.cache` |
| Modify | `gui/src/main/kotlin/darkan/editor/gui/controller/MainController.kt` | Use new `App.cache` |
| Modify | `gui/src/main/kotlin/darkan/editor/gui/Settings.kt` | Use new `App.cache` |
| Modify | `plugin/src/main/kotlin/darkan/editor/plugin/extension/ConfigExtension.kt` | Use `CacheSystem` |
| Modify | `plugins/itemdef-plugin/src/main/java/darkan/editor/plugin/Controller.kt` | Use `App.cache` |
| Modify | `plugins/objdef-plugin/src/main/java/darkan/editor/plugin/Controller.kt` | Use `App.cache` |
| Modify | `plugins/npc-plugin/src/main/java/darkan/editor/plugin/Controller.kt` | Use `App.cache` |
| Modify | `plugins/varbit-plugin/src/main/java/darkan/editor/plugin/Controller.kt` | Use `App.cache` |
| Modify | `plugins/sprite-plugin/src/main/java/darkan/editor/plugin/Controller.kt` | Use `App.cache` |
| Modify | `plugins/texture-plugin/src/main/java/darkan/editor/plugin/Controller.kt` | Use `App.cache` |
| Modify | `plugins/archive-plugin/src/main/java/darkan/editor/plugin/Controller.kt` | Use `App.cache` |
| Create | `fs/src/test/java/darkan/editor/fs/CacheSystemFactoryTest.java` | Factory tests |
| Create | `fs/src/test/java/darkan/editor/fs/ModernCacheSystemTest.java` | Modern impl tests |

---

### Task 1: Add Dependencies and Test Infrastructure

**Files:**
- Modify: `fs/build.gradle`

**Interfaces:**
- Consumes: nothing
- Produces: Displee library on classpath, JUnit 5 test framework available in `fs` module

- [ ] **Step 1: Update fs/build.gradle**

```groovy

description = 'Darkan Editor RS2 File System'

dependencies {
  implementation(project(":io"))
  implementation(project(":util"))
  implementation 'com.displee:rs-cache-library:7.3.0'

  testImplementation 'org.junit.jupiter:junit-jupiter:5.11.4'
  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

sourceSets {
  main.java.srcDirs = ['src/main/java', 'src/example/java']
}

test {
  useJUnitPlatform()
}
```

- [ ] **Step 2: Verify dependency resolution**

Run: `cd /home/dev/prototypes/Darkan-editor && ./gradlew :fs:dependencies --configuration compileClasspath`
Expected: `com.displee:rs-cache-library:7.3.0` appears in the output

- [ ] **Step 3: Commit**

```bash
git add fs/build.gradle
git commit -m "build: add Displee rs-cache-library and JUnit 5 to fs module"
```

---

### Task 2: Core Interfaces and Types

**Files:**
- Create: `fs/src/main/java/darkan/editor/fs/CacheConstants.java`
- Create: `fs/src/main/java/darkan/editor/fs/CacheFormat.java`
- Create: `fs/src/main/java/darkan/editor/fs/CacheArchive.java`
- Create: `fs/src/main/java/darkan/editor/fs/CacheIndex.java`
- Create: `fs/src/main/java/darkan/editor/fs/CacheSystem.java`
- Create: `fs/src/main/java/darkan/editor/fs/CacheSystemFactory.java`

**Interfaces:**
- Consumes: nothing
- Produces: `CacheSystem`, `CacheIndex`, `CacheArchive` interfaces; `CacheFormat` enum; `CacheConstants` class; `CacheSystemFactory.open(Path): CacheSystem`

- [ ] **Step 1: Create CacheConstants.java**

```java
package darkan.editor.fs;

public final class CacheConstants {

    public static final String LEGACY_DATA_FILE = "main_file_cache.dat";
    public static final String MODERN_DATA_FILE = "main_file_cache.dat2";
    public static final String INDEX_FILE_PREFIX = "main_file_cache.idx";

    private CacheConstants() {}
}
```

- [ ] **Step 2: Create CacheFormat.java**

```java
package darkan.editor.fs;

public enum CacheFormat {
    LEGACY_317,
    MODERN
}
```

- [ ] **Step 3: Create CacheArchive.java**

```java
package darkan.editor.fs;

import java.nio.ByteBuffer;

public interface CacheArchive {

    int getId();

    int getFileCount();

    int[] getFileIds();

    ByteBuffer readFile(int fileId);

    boolean writeFile(int fileId, byte[] data);
}
```

- [ ] **Step 4: Create CacheIndex.java**

```java
package darkan.editor.fs;

public interface CacheIndex {

    int getId();

    int getArchiveCount();

    CacheArchive getArchive(int archiveId);

    int[] getArchiveIds();
}
```

- [ ] **Step 5: Create CacheSystem.java**

```java
package darkan.editor.fs;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public interface CacheSystem extends Closeable {

    boolean load();

    boolean isLoaded();

    void close();

    Path getRoot();

    int getIndexCount();

    CacheIndex getIndex(int indexId);

    ByteBuffer readFile(int indexId, int archiveId, int fileId);

    ByteBuffer readFile(int indexId, int archiveId);

    boolean writeFile(int indexId, int archiveId, int fileId, byte[] data);

    boolean writeFile(int indexId, int archiveId, byte[] data);

    CacheFormat getFormat();
}
```

- [ ] **Step 6: Create CacheSystemFactory.java**

```java
package darkan.editor.fs;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CacheSystemFactory {

    private CacheSystemFactory() {}

    public static CacheSystem open(Path path) {
        if (Files.exists(path.resolve(CacheConstants.MODERN_DATA_FILE))) {
            return new ModernCacheSystem(path);
        } else if (Files.exists(path.resolve(CacheConstants.LEGACY_DATA_FILE))) {
            return new LegacyCacheSystem(path);
        }
        throw new IllegalArgumentException("No recognized cache format at: " + path);
    }
}
```

Note: This won't compile until Tasks 3 and 4 create `LegacyCacheSystem` and `ModernCacheSystem`. That's expected — we commit together after Task 4.

- [ ] **Step 7: Verify files compile (will fail until Task 3+4, just check syntax)**

Run: `cd /home/dev/prototypes/Darkan-editor && javac --version`
Expected: javac 21.x.x (confirms toolchain)

- [ ] **Step 8: Commit**

```bash
git add fs/src/main/java/darkan/editor/fs/CacheConstants.java \
        fs/src/main/java/darkan/editor/fs/CacheFormat.java \
        fs/src/main/java/darkan/editor/fs/CacheArchive.java \
        fs/src/main/java/darkan/editor/fs/CacheIndex.java \
        fs/src/main/java/darkan/editor/fs/CacheSystem.java \
        fs/src/main/java/darkan/editor/fs/CacheSystemFactory.java
git commit -m "feat: add CacheSystem interface, supporting types, and factory"
```

---

### Task 3: Legacy Implementation

**Files:**
- Create: `fs/src/main/java/darkan/editor/fs/LegacyCacheArchive.java`
- Create: `fs/src/main/java/darkan/editor/fs/LegacyCacheIndex.java`
- Create: `fs/src/main/java/darkan/editor/fs/LegacyCacheSystem.java`

**Interfaces:**
- Consumes: `CacheSystem`, `CacheIndex`, `CacheArchive` (from Task 2); existing `RSFileSystem`, `RSFileStore`, `RSArchive`
- Produces: `LegacyCacheSystem` implements `CacheSystem`; `LegacyCacheIndex` implements `CacheIndex`; `LegacyCacheArchive` implements `CacheArchive`

- [ ] **Step 1: Create LegacyCacheArchive.java**

For index 0 (archive store), this wraps an `RSArchive` and delegates file reads to archive entries by index. For flat stores (indices 1+), this wraps a single raw `ByteBuffer` as the sole file (fileId=0).

```java
package darkan.editor.fs;

import java.nio.ByteBuffer;

public final class LegacyCacheArchive implements CacheArchive {

    private final int id;
    private final RSArchive archive;
    private final ByteBuffer rawData;

    LegacyCacheArchive(int id, RSArchive archive) {
        this.id = id;
        this.archive = archive;
        this.rawData = null;
    }

    LegacyCacheArchive(int id, ByteBuffer rawData) {
        this.id = id;
        this.archive = null;
        this.rawData = rawData;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getFileCount() {
        if (archive != null) {
            return archive.getEntryCount();
        }
        return rawData != null ? 1 : 0;
    }

    @Override
    public int[] getFileIds() {
        if (archive != null) {
            int count = archive.getEntryCount();
            int[] ids = new int[count];
            for (int i = 0; i < count; i++) {
                ids[i] = i;
            }
            return ids;
        }
        return rawData != null ? new int[]{0} : new int[0];
    }

    @Override
    public ByteBuffer readFile(int fileId) {
        if (archive != null) {
            try {
                RSArchive.ArchiveEntry entry = archive.getEntryAt(fileId);
                return ByteBuffer.wrap(entry.getData());
            } catch (Exception e) {
                return null;
            }
        }
        if (rawData != null && fileId == 0) {
            return rawData.duplicate();
        }
        return null;
    }

    @Override
    public boolean writeFile(int fileId, byte[] data) {
        if (archive != null) {
            try {
                return archive.writeFile(fileId, data);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public RSArchive getRSArchive() {
        return archive;
    }
}
```

- [ ] **Step 2: Create LegacyCacheIndex.java**

```java
package darkan.editor.fs;

import java.nio.ByteBuffer;

public final class LegacyCacheIndex implements CacheIndex {

    private final int id;
    private final RSFileStore store;

    LegacyCacheIndex(int id, RSFileStore store) {
        this.id = id;
        this.store = store;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getArchiveCount() {
        return store.getFileCount();
    }

    @Override
    public CacheArchive getArchive(int archiveId) {
        if (id == RSFileStore.ARCHIVE_FILE_STORE) {
            ByteBuffer data = store.readFile(archiveId);
            if (data == null) {
                return null;
            }
            try {
                RSArchive archive = RSArchive.decode(data);
                return new LegacyCacheArchive(archiveId, archive);
            } catch (Exception e) {
                return null;
            }
        }
        ByteBuffer data = store.readFile(archiveId);
        return new LegacyCacheArchive(archiveId, data);
    }

    @Override
    public int[] getArchiveIds() {
        int count = store.getFileCount();
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            ids[i] = i;
        }
        return ids;
    }

    public RSFileStore getStore() {
        return store;
    }
}
```

- [ ] **Step 3: Create LegacyCacheSystem.java**

```java
package darkan.editor.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public final class LegacyCacheSystem implements CacheSystem {

    private final RSFileSystem fileSystem;
    private final Path root;

    public LegacyCacheSystem(Path root) {
        this.root = root;
        this.fileSystem = new RSFileSystem();
        this.fileSystem.setRoot(root);
    }

    @Override
    public boolean load() {
        return fileSystem.load();
    }

    @Override
    public boolean isLoaded() {
        return fileSystem.isLoaded();
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }

    @Override
    public Path getRoot() {
        return root;
    }

    @Override
    public int getIndexCount() {
        return fileSystem.getStoreCount();
    }

    @Override
    public CacheIndex getIndex(int indexId) {
        RSFileStore store = fileSystem.getStore(indexId);
        if (store == null) {
            return null;
        }
        return new LegacyCacheIndex(indexId, store);
    }

    @Override
    public ByteBuffer readFile(int indexId, int archiveId, int fileId) {
        CacheIndex index = getIndex(indexId);
        if (index == null) {
            return null;
        }
        CacheArchive archive = index.getArchive(archiveId);
        if (archive == null) {
            return null;
        }
        return archive.readFile(fileId);
    }

    @Override
    public ByteBuffer readFile(int indexId, int archiveId) {
        RSFileStore store = fileSystem.getStore(indexId);
        if (store == null) {
            return null;
        }
        return store.readFile(archiveId);
    }

    @Override
    public boolean writeFile(int indexId, int archiveId, int fileId, byte[] data) {
        CacheIndex index = getIndex(indexId);
        if (index == null) {
            return false;
        }
        CacheArchive archive = index.getArchive(archiveId);
        if (archive == null) {
            return false;
        }
        return archive.writeFile(fileId, data);
    }

    @Override
    public boolean writeFile(int indexId, int archiveId, byte[] data) {
        RSFileStore store = fileSystem.getStore(indexId);
        if (store == null) {
            return false;
        }
        return store.writeFile(archiveId, data);
    }

    @Override
    public CacheFormat getFormat() {
        return CacheFormat.LEGACY_317;
    }

    public RSFileSystem getFileSystem() {
        return fileSystem;
    }
}
```

- [ ] **Step 4: Make RSFileSystem constructor accessible**

The existing `RSFileSystem` has a private constructor and singleton pattern. We need to allow `LegacyCacheSystem` to create instances. Change the constructor visibility from `private` to package-private in `fs/src/main/java/darkan/editor/fs/RSFileSystem.java`:

Change line 22:
```java
private RSFileSystem() {}
```
to:
```java
RSFileSystem() {}
```

- [ ] **Step 5: Verify compilation**

Run: `cd /home/dev/prototypes/Darkan-editor && ./gradlew :fs:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add fs/src/main/java/darkan/editor/fs/LegacyCacheArchive.java \
        fs/src/main/java/darkan/editor/fs/LegacyCacheIndex.java \
        fs/src/main/java/darkan/editor/fs/LegacyCacheSystem.java \
        fs/src/main/java/darkan/editor/fs/RSFileSystem.java
git commit -m "feat: add LegacyCacheSystem implementation wrapping existing v317 classes"
```

---

### Task 4: Modern Implementation (Displee Wrapper)

**Files:**
- Create: `fs/src/main/java/darkan/editor/fs/ModernCacheArchive.java`
- Create: `fs/src/main/java/darkan/editor/fs/ModernCacheIndex.java`
- Create: `fs/src/main/java/darkan/editor/fs/ModernCacheSystem.java`

**Interfaces:**
- Consumes: `CacheSystem`, `CacheIndex`, `CacheArchive` (from Task 2); Displee `CacheLibrary`, `Index`, `Archive`, `File` classes
- Produces: `ModernCacheSystem` implements `CacheSystem`; `ModernCacheIndex` implements `CacheIndex`; `ModernCacheArchive` implements `CacheArchive`

- [ ] **Step 1: Create ModernCacheArchive.java**

```java
package darkan.editor.fs;

import com.displee.cache.index.archive.Archive;
import com.displee.cache.index.archive.file.File;

import java.nio.ByteBuffer;

public final class ModernCacheArchive implements CacheArchive {

    private final Archive archive;

    ModernCacheArchive(Archive archive) {
        this.archive = archive;
    }

    @Override
    public int getId() {
        return archive.getId();
    }

    @Override
    public int getFileCount() {
        return archive.fileIds().length;
    }

    @Override
    public int[] getFileIds() {
        return archive.fileIds();
    }

    @Override
    public ByteBuffer readFile(int fileId) {
        File file = archive.file(fileId);
        if (file == null) {
            return null;
        }
        byte[] data = file.getData();
        if (data == null) {
            return null;
        }
        return ByteBuffer.wrap(data);
    }

    @Override
    public boolean writeFile(int fileId, byte[] data) {
        archive.addFile(fileId, data);
        return true;
    }

    public Archive getDispleeArchive() {
        return archive;
    }
}
```

- [ ] **Step 2: Create ModernCacheIndex.java**

```java
package darkan.editor.fs;

import com.displee.cache.index.Index;
import com.displee.cache.index.archive.Archive;

import java.nio.ByteBuffer;

public final class ModernCacheIndex implements CacheIndex {

    private final Index index;

    ModernCacheIndex(Index index) {
        this.index = index;
    }

    @Override
    public int getId() {
        return index.getId();
    }

    @Override
    public int getArchiveCount() {
        return index.archiveIds().length;
    }

    @Override
    public CacheArchive getArchive(int archiveId) {
        Archive archive = index.archive(archiveId);
        if (archive == null) {
            return null;
        }
        return new ModernCacheArchive(archive);
    }

    @Override
    public int[] getArchiveIds() {
        return index.archiveIds();
    }

    public Index getDispleeIndex() {
        return index;
    }
}
```

- [ ] **Step 3: Create ModernCacheSystem.java**

```java
package darkan.editor.fs;

import com.displee.cache.CacheLibrary;
import com.displee.cache.index.Index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public final class ModernCacheSystem implements CacheSystem {

    private final Path root;
    private CacheLibrary library;
    private boolean loaded;

    public ModernCacheSystem(Path root) {
        this.root = root;
    }

    @Override
    public boolean load() {
        try {
            library = CacheLibrary.create(root.toString());
            loaded = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void close() throws IOException {
        if (library != null) {
            library.close();
            loaded = false;
        }
    }

    @Override
    public Path getRoot() {
        return root;
    }

    @Override
    public int getIndexCount() {
        if (library == null) {
            return 0;
        }
        return library.indices().length;
    }

    @Override
    public CacheIndex getIndex(int indexId) {
        if (library == null) {
            return null;
        }
        Index index = library.index(indexId);
        if (index == null) {
            return null;
        }
        return new ModernCacheIndex(index);
    }

    @Override
    public ByteBuffer readFile(int indexId, int archiveId, int fileId) {
        if (library == null) {
            return null;
        }
        byte[] data = library.data(indexId, archiveId, fileId);
        if (data == null) {
            return null;
        }
        return ByteBuffer.wrap(data);
    }

    @Override
    public ByteBuffer readFile(int indexId, int archiveId) {
        if (library == null) {
            return null;
        }
        byte[] data = library.data(indexId, archiveId);
        if (data == null) {
            return null;
        }
        return ByteBuffer.wrap(data);
    }

    @Override
    public boolean writeFile(int indexId, int archiveId, int fileId, byte[] data) {
        if (library == null) {
            return false;
        }
        try {
            library.put(indexId, archiveId, fileId, data);
            library.index(indexId).update();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean writeFile(int indexId, int archiveId, byte[] data) {
        if (library == null) {
            return false;
        }
        try {
            library.put(indexId, archiveId, data);
            library.index(indexId).update();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public CacheFormat getFormat() {
        return CacheFormat.MODERN;
    }

    public CacheLibrary getLibrary() {
        return library;
    }
}
```

- [ ] **Step 4: Verify full fs module compiles**

Run: `cd /home/dev/prototypes/Darkan-editor && ./gradlew :fs:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add fs/src/main/java/darkan/editor/fs/ModernCacheArchive.java \
        fs/src/main/java/darkan/editor/fs/ModernCacheIndex.java \
        fs/src/main/java/darkan/editor/fs/ModernCacheSystem.java
git commit -m "feat: add ModernCacheSystem implementation wrapping Displee CacheLibrary"
```

---

### Task 5: Tests for Modern Implementation

**Files:**
- Create: `fs/src/test/java/darkan/editor/fs/CacheSystemFactoryTest.java`
- Create: `fs/src/test/java/darkan/editor/fs/ModernCacheSystemTest.java`

**Interfaces:**
- Consumes: `CacheSystemFactory.open(Path)`, `ModernCacheSystem`, `CacheIndex`, `CacheArchive` (from Tasks 2-4)
- Produces: Test suite validating factory detection and modern cache reading

- [ ] **Step 1: Create CacheSystemFactoryTest.java**

```java
package darkan.editor.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CacheSystemFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    void openDetectsModernFormat() throws IOException {
        Files.createFile(tempDir.resolve(CacheConstants.MODERN_DATA_FILE));
        Files.createFile(tempDir.resolve(CacheConstants.INDEX_FILE_PREFIX + "0"));
        Files.createFile(tempDir.resolve(CacheConstants.INDEX_FILE_PREFIX + "255"));

        CacheSystem cache = CacheSystemFactory.open(tempDir);
        assertNotNull(cache);
        assertEquals(CacheFormat.MODERN, cache.getFormat());
    }

    @Test
    void openDetectsLegacyFormat() throws IOException {
        Files.createFile(tempDir.resolve(CacheConstants.LEGACY_DATA_FILE));
        Files.createFile(tempDir.resolve(CacheConstants.INDEX_FILE_PREFIX + "0"));

        CacheSystem cache = CacheSystemFactory.open(tempDir);
        assertNotNull(cache);
        assertEquals(CacheFormat.LEGACY_317, cache.getFormat());
    }

    @Test
    void openPrefersModernWhenBothExist() throws IOException {
        Files.createFile(tempDir.resolve(CacheConstants.MODERN_DATA_FILE));
        Files.createFile(tempDir.resolve(CacheConstants.LEGACY_DATA_FILE));
        Files.createFile(tempDir.resolve(CacheConstants.INDEX_FILE_PREFIX + "0"));

        CacheSystem cache = CacheSystemFactory.open(tempDir);
        assertEquals(CacheFormat.MODERN, cache.getFormat());
    }

    @Test
    void openThrowsForUnrecognizedFormat() {
        assertThrows(IllegalArgumentException.class, () -> CacheSystemFactory.open(tempDir));
    }
}
```

- [ ] **Step 2: Create ModernCacheSystemTest.java**

```java
package darkan.editor.fs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModernCacheSystemTest {

    private static final Path CACHE_PATH = Path.of("/home/dev/prototypes/cache");

    private ModernCacheSystem cache;

    @BeforeEach
    void setUp() {
        cache = new ModernCacheSystem(CACHE_PATH);
        assertTrue(cache.load(), "Failed to load modern cache at " + CACHE_PATH);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (cache != null) {
            cache.close();
        }
    }

    @Test
    void loadReportsCorrectFormat() {
        assertEquals(CacheFormat.MODERN, cache.getFormat());
    }

    @Test
    void loadReportsLoaded() {
        assertTrue(cache.isLoaded());
    }

    @Test
    void getIndexCountIsPositive() {
        assertTrue(cache.getIndexCount() > 0, "Expected at least one index");
    }

    @Test
    void getIndexReturnsNonNull() {
        CacheIndex index = cache.getIndex(0);
        assertNotNull(index, "Index 0 should exist");
        assertEquals(0, index.getId());
    }

    @Test
    void indexHasArchives() {
        CacheIndex index = cache.getIndex(0);
        assertNotNull(index);
        assertTrue(index.getArchiveCount() > 0, "Index 0 should have archives");
    }

    @Test
    void readFileReturnsData() {
        CacheIndex index = cache.getIndex(0);
        assertNotNull(index);
        int[] archiveIds = index.getArchiveIds();
        assertTrue(archiveIds.length > 0);

        CacheArchive archive = index.getArchive(archiveIds[0]);
        assertNotNull(archive);

        int[] fileIds = archive.getFileIds();
        if (fileIds.length > 0) {
            ByteBuffer data = archive.readFile(fileIds[0]);
            assertNotNull(data);
            assertTrue(data.remaining() > 0);
        }
    }

    @Test
    void readFileViaSystemShortcut() {
        CacheIndex index = cache.getIndex(0);
        assertNotNull(index);
        int[] archiveIds = index.getArchiveIds();
        assertTrue(archiveIds.length > 0);

        ByteBuffer data = cache.readFile(0, archiveIds[0]);
        assertNotNull(data);
    }

    @Test
    void closeMarksUnloaded() throws Exception {
        cache.close();
        assertFalse(cache.isLoaded());
    }
}
```

- [ ] **Step 3: Run tests**

Run: `cd /home/dev/prototypes/Darkan-editor && ./gradlew :fs:test`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add fs/src/test/java/darkan/editor/fs/CacheSystemFactoryTest.java \
        fs/src/test/java/darkan/editor/fs/ModernCacheSystemTest.java
git commit -m "test: add tests for CacheSystemFactory and ModernCacheSystem"
```

---

### Task 6: Migrate App and GUI Consumers

**Files:**
- Modify: `gui/src/main/kotlin/darkan/editor/gui/App.kt`
- Modify: `gui/src/main/kotlin/darkan/editor/gui/event/LoadCacheEvent.kt`
- Modify: `gui/src/main/kotlin/darkan/editor/gui/controller/BaseController.kt`
- Modify: `gui/src/main/kotlin/darkan/editor/gui/controller/MainController.kt`
- Modify: `gui/src/main/kotlin/darkan/editor/gui/Settings.kt`

**Interfaces:**
- Consumes: `CacheSystem`, `CacheSystemFactory.open(Path)`, `CacheFormat` (from Tasks 2-4)
- Produces: `App.cache: CacheSystem?` replacing `App.fs: RSFileSystem`; all GUI consumers updated

- [ ] **Step 1: Update App.kt**

Replace the current companion object contents:

```kotlin
package darkan.editor.gui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.StageStyle
import darkan.editor.fs.CacheSystem
import darkan.editor.fs.CacheSystemFactory
import java.nio.file.Path

class App : Application() {

    override fun init() {
        Settings.load()
    }

    override fun start(stage: Stage) {
        mainStage = stage

        val root : Parent = FXMLLoader.load(App::class.java.getResource("/scenes/StoreScene.fxml"))
        stage.title = "Darkan Editor [build $VERSION]"
        val scene = Scene(root)
        scene.stylesheets.add(App::class.java.getResource("/style.css").toExternalForm())
        stage.scene = scene
        stage.icons.add(Image(App::class.java.getResourceAsStream("/icons/icon.png")))
        stage.centerOnScreen()
        stage.isResizable = false
        stage.initStyle(StageStyle.UNDECORATED)
        stage.show()
    }

    override fun stop() {
        val c = cache
        if (c != null && c.isLoaded) {
            Settings.save(c.root)
        }
    }

    companion object {
        const val VERSION = "3.1.0"

        var cache: CacheSystem? = null
            private set

        lateinit var mainStage : Stage

        fun openCache(path: Path) {
            cache?.close()
            val system = CacheSystemFactory.open(path)
            if (system.load()) {
                cache = system
            }
        }

        fun closeCache() {
            cache?.close()
            cache = null
        }

        @JvmStatic
        fun main(args : Array<String>) {
            launch(App::class.java)
        }
    }

}
```

- [ ] **Step 2: Update LoadCacheEvent.kt**

```kotlin
package darkan.editor.gui.event

import darkan.editor.fs.CacheSystem

class LoadCacheEvent(val cache: CacheSystem)
```

- [ ] **Step 3: Update BaseController.kt**

Replace the `openFS()` method body. Change:
```kotlin
App.fs.root = selectedDir.toPath()
if (!App.fs.load()) {
    return
}
```
to:
```kotlin
App.openCache(selectedDir.toPath())
if (App.cache == null || !App.cache!!.isLoaded) {
    return
}
```

Replace `App.fs.isLoaded` with `App.cache?.isLoaded == true` everywhere in the file.

Replace `clearProgram()`:
```kotlin
private fun clearProgram() {
    if (App.cache?.isLoaded == true) {
        App.closeCache()
    }
    onClear()
}
```

- [ ] **Step 4: Update MainController.kt**

Replace all occurrences of `App.fs` with `App.cache`:
- `App.fs.isLoaded` → `App.cache?.isLoaded == true`
- `App.fs.getStore(id)` → `App.cache!!.getIndex(id)` (note: returns `CacheIndex` not `RSFileStore` — this requires adjusting the usage pattern)
- `App.fs.root` → `App.cache!!.root`
- `App.fs.reset()` / `App.fs.load()` → `App.closeCache()` / `App.openCache(path)`
- `App.fs.defragment()` → keep only for legacy (wrap in format check)
- `App.fs.storeCount` → `App.cache!!.indexCount`
- `App.fs.createStore(nextId)` → legacy-only operation (wrap in format check)
- `PluginManager.post(LoadCacheEvent(App.fs))` → `PluginManager.post(LoadCacheEvent(App.cache!!))`

For operations that are legacy-specific (defragment, createStore), wrap them:
```kotlin
val c = App.cache
if (c is LegacyCacheSystem) {
    c.getFileSystem().defragment()
}
```

For the store entry iteration that calls `store.readFile(i)` and `store.fileCount`, use:
```kotlin
val index = App.cache!!.getIndex(id)
val archiveIds = index.archiveIds
for (archiveId in archiveIds) {
    val data = App.cache!!.readFile(id, archiveId)
    // ...
}
```

- [ ] **Step 5: Update Settings.kt**

Replace:
- `App.fs.isLoaded` → `App.cache?.isLoaded == true`
- `App.fs.root` → `App.cache?.root`

- [ ] **Step 6: Verify GUI module compiles**

Run: `cd /home/dev/prototypes/Darkan-editor && ./gradlew :gui:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add gui/src/main/kotlin/darkan/editor/gui/App.kt \
        gui/src/main/kotlin/darkan/editor/gui/event/LoadCacheEvent.kt \
        gui/src/main/kotlin/darkan/editor/gui/controller/BaseController.kt \
        gui/src/main/kotlin/darkan/editor/gui/controller/MainController.kt \
        gui/src/main/kotlin/darkan/editor/gui/Settings.kt
git commit -m "refactor: migrate App and GUI controllers to CacheSystem interface"
```

---

### Task 7: Migrate ConfigExtension and Plugins

**Files:**
- Modify: `plugin/src/main/kotlin/darkan/editor/plugin/extension/ConfigExtension.kt`
- Modify: `plugins/itemdef-plugin/src/main/java/darkan/editor/plugin/Controller.kt`
- Modify: `plugins/objdef-plugin/src/main/java/darkan/editor/plugin/Controller.kt`
- Modify: `plugins/npc-plugin/src/main/java/darkan/editor/plugin/Controller.kt`
- Modify: `plugins/varbit-plugin/src/main/java/darkan/editor/plugin/Controller.kt`
- Modify: `plugins/sprite-plugin/src/main/java/darkan/editor/plugin/Controller.kt`
- Modify: `plugins/texture-plugin/src/main/java/darkan/editor/plugin/Controller.kt`
- Modify: `plugins/archive-plugin/src/main/java/darkan/editor/plugin/Controller.kt`

**Interfaces:**
- Consumes: `CacheSystem`, `App.cache` (from Tasks 2-6)
- Produces: All plugins compile and work against both cache formats

- [ ] **Step 1: Update ConfigExtension.kt**

In `onSave`, replace:
```kotlin
val store = RSFileSystem.getInstance().getStore(getStoreId()) ?: return
val encoded = archive.encode() ?: return
if (store.writeFile(getFileId(), encoded)) {
```
with:
```kotlin
val cache = App.cache ?: return
if (!cache.writeFile(getStoreId(), getFileId(), encoded)) {
    return
}
```

Full updated `onSave`:
```kotlin
open fun onSave(list: ObservableList<KeyModel>, archive: RSArchive) {
    try {
        if (list.isEmpty()) {
            return
        }

        val metaBuf = RSBuffer.init()
        val dataBuf = RSBuffer.init()

        writeLength(dataBuf, list.size)

        if (useMetaFile()) {
            writeLength(metaBuf, list.size)
        }

        for (i in 0 until list.size) {
            val item = list[i]
            val instance = item.instance as ConfigExtension
            item.map.mapToInstance(instance)

            var lastPos = dataBuf.position

            instance.encode(dataBuf)

            if (useMetaFile()) {
                writeOffset(metaBuf, dataBuf, lastPos)
            }
        }

        archive.writeFile(getDataFileName(), dataBuf.toArray())

        if (useMetaFile()) {
            archive.writeFile(getMetaFileName(), metaBuf.toArray())
        }

        val cache = App.cache ?: return
        val encoded = archive.encode() ?: return

        if (cache.writeFile(getStoreId(), getFileId(), encoded)) {
            val alert = Alert(Alert.AlertType.INFORMATION)
            alert.title = "Info"
            alert.headerText = "Success!"
            Platform.runLater { alert.show() }
        }

    } catch (ex: IOException) {
        ex.printStackTrace()
    }
}
```

Remove the import of `RSFileSystem` from `ConfigExtension.kt`. Add import for `App`:
```kotlin
import darkan.editor.gui.App
```

- [ ] **Step 2: Update itemdef, objdef, npc, varbit plugin Controllers**

These four plugins follow an identical pattern. In each file, replace:
```kotlin
import darkan.editor.fs.RSArchive
// ...
val archive = App.fs.getArchive(RSArchive.CONFIG_ARCHIVE)
```
with:
```kotlin
import darkan.editor.fs.CacheSystem
import darkan.editor.fs.RSArchive
import darkan.editor.fs.LegacyCacheIndex
// ...
val cache = App.cache ?: return
val index = cache.getIndex(0) as? LegacyCacheIndex
val archive = if (index != null) {
    RSArchive.decode(index.getStore().readFile(RSArchive.CONFIG_ARCHIVE))
} else {
    val data = cache.readFile(0, RSArchive.CONFIG_ARCHIVE) ?: return
    RSArchive.decode(data)
}
```

Also replace `App.fs` references in `onSave` methods identically.

Remove `import darkan.editor.fs.RSFileSystem` if present. The `App.fs` references become `App.cache`.

- [ ] **Step 3: Update sprite-plugin Controller**

Replace:
```kotlin
val archive = App.fs.getArchive(RSFileStore.ARCHIVE_FILE_STORE, RSArchive.MEDIA_ARCHIVE)
```
with:
```kotlin
val cache = App.cache ?: return
val data = cache.readFile(0, RSArchive.MEDIA_ARCHIVE) ?: return
val archive = RSArchive.decode(data)
```

Replace:
```kotlin
val store = App.fs.getStore(RSFileStore.ARCHIVE_FILE_STORE)
store.writeFile(RSArchive.MEDIA_ARCHIVE, encoded)
```
with:
```kotlin
val cache = App.cache ?: return
cache.writeFile(0, RSArchive.MEDIA_ARCHIVE, encoded)
```

- [ ] **Step 4: Update texture-plugin Controller**

Same pattern as sprite-plugin:
```kotlin
val cache = App.cache ?: return
val data = cache.readFile(0, RSArchive.TEXTURE_ARCHIVE) ?: return
val archive = RSArchive.decode(data)
```

Write:
```kotlin
val cache = App.cache ?: return
cache.writeFile(0, RSArchive.TEXTURE_ARCHIVE, encoded)
```

- [ ] **Step 5: Update archive-plugin Controller**

Replace:
```kotlin
val store = App.fs.getStore(RSFileStore.ARCHIVE_FILE_STORE) ?: return false
```
with:
```kotlin
val cache = App.cache ?: return false
```

For the pack operation:
```kotlin
val encoded = archive.encode() ?: return false
if (cache.writeFile(0, selectedArchive.id, encoded)) {
```

For the populate loop:
```kotlin
val cache = App.cache ?: return false
val index = cache.getIndex(0) ?: return false
val archiveIds = index.archiveIds
for (archiveId in archiveIds) {
    val data = cache.readFile(0, archiveId) ?: continue
    if (data.capacity() == 0) continue
    val archive = RSArchive.decode(data) ?: continue
    var name = Settings.getStoreEntryReferenceName(0, archiveId)
    if (name == null) name = archiveId.toString()
    // ...
}
```

Replace `App.fs.isLoaded` with `App.cache?.isLoaded == true`.

- [ ] **Step 6: Verify all plugins compile**

Run: `cd /home/dev/prototypes/Darkan-editor && ./gradlew compileKotlin compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add plugin/src/main/kotlin/darkan/editor/plugin/extension/ConfigExtension.kt \
        plugins/itemdef-plugin/src/main/java/darkan/editor/plugin/Controller.kt \
        plugins/objdef-plugin/src/main/java/darkan/editor/plugin/Controller.kt \
        plugins/npc-plugin/src/main/java/darkan/editor/plugin/Controller.kt \
        plugins/varbit-plugin/src/main/java/darkan/editor/plugin/Controller.kt \
        plugins/sprite-plugin/src/main/java/darkan/editor/plugin/Controller.kt \
        plugins/texture-plugin/src/main/java/darkan/editor/plugin/Controller.kt \
        plugins/archive-plugin/src/main/java/darkan/editor/plugin/Controller.kt
git commit -m "refactor: migrate all plugins to CacheSystem interface"
```

---

### Task 8: Final Integration Verification

**Files:**
- No new files

**Interfaces:**
- Consumes: All prior tasks
- Produces: Confirmed full build and test pass

- [ ] **Step 1: Full build**

Run: `cd /home/dev/prototypes/Darkan-editor && ./gradlew clean build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run tests**

Run: `cd /home/dev/prototypes/Darkan-editor && ./gradlew :fs:test`
Expected: All tests PASS

- [ ] **Step 3: Verify modern cache loads**

Run a quick test that the factory correctly opens the Darkan cache:
```bash
cd /home/dev/prototypes/Darkan-editor && ./gradlew :fs:test --tests "darkan.editor.fs.ModernCacheSystemTest"
```
Expected: All tests PASS

- [ ] **Step 4: Commit any final fixes if needed**

```bash
git status
# If clean, no commit needed
```
