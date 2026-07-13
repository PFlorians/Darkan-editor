# Cache System Abstraction Design

## Summary

Introduce an interface-based cache system abstraction into the `darkan.editor.fs` module that supports both legacy v317 caches and modern (post-317) caches. The modern implementation is backed by Displee's `rs-cache-library`. A factory with format auto-detection allows the editor to open either format transparently.

## Goals

- Support both v317 and modern RS cache formats side-by-side
- Model the interface around the modern cache's richer hierarchy (index → archive → file)
- Adapt the legacy v317 implementation to fit the modern-shaped interface
- Allow switching between caches within a single editor session
- Preserve existing `RSFileSystem`, `RSFileStore`, `RSArchive` as internal implementation details

## Non-Goals

- Removing or rewriting the existing v317 classes
- Supporting write-back for every possible modern cache operation on day one
- Plugin API changes beyond swapping `RSFileSystem` for `CacheSystem`

## Architecture

### Module: `fs`

All code lives in `darkan.editor.fs`. The Displee library is added as a dependency to this module.

### Dependency

```groovy
// fs/build.gradle
implementation 'com.displee:rs-cache-library:7.1.1'
```

Version to be confirmed at implementation time against Maven Central.

### Detection Constants

```java
public final class CacheConstants {
    public static final String LEGACY_DATA_FILE = "main_file_cache.dat";
    public static final String MODERN_DATA_FILE = "main_file_cache.dat2";
    public static final String INDEX_FILE_PREFIX = "main_file_cache.idx";

    private CacheConstants() {}
}
```

### CacheFormat Enum

```java
public enum CacheFormat {
    LEGACY_317,
    MODERN
}
```

### CacheSystem Interface

```java
public interface CacheSystem extends Closeable {

    // Lifecycle
    boolean load();
    boolean isLoaded();
    void close();
    Path getRoot();

    // Index level
    int getIndexCount();
    CacheIndex getIndex(int indexId);

    // Read a specific file within an archive
    ByteBuffer readFile(int indexId, int archiveId, int fileId);
    // Read an entire archive as raw bytes (single-file archives or legacy flat stores)
    ByteBuffer readFile(int indexId, int archiveId);
    // Write a specific file within an archive
    boolean writeFile(int indexId, int archiveId, int fileId, byte[] data);
    // Write raw bytes to an archive (single-file archives or legacy flat stores)
    boolean writeFile(int indexId, int archiveId, byte[] data);

    // Metadata
    CacheFormat getFormat();
}
```

### CacheIndex Interface

```java
public interface CacheIndex {
    int getId();
    int getArchiveCount();
    CacheArchive getArchive(int archiveId);
    int[] getArchiveIds();
}
```

### CacheArchive Interface

```java
public interface CacheArchive {
    int getId();
    int getFileCount();
    int[] getFileIds();
    ByteBuffer readFile(int fileId);
    boolean writeFile(int fileId, byte[] data);
}
```

### CacheSystemFactory

```java
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

## Implementations

### ModernCacheSystem

- Wraps Displee's `CacheLibrary`
- `load()` calls `CacheLibrary.create(path)`
- `CacheIndex` wraps Displee's `Index`
- `CacheArchive` wraps Displee's `Archive`
- File read/write delegates directly to Displee's API

### LegacyCacheSystem

- Wraps the existing `RSFileSystem` / `RSFileStore` / `RSArchive` classes internally
- `CacheIndex` wraps an `RSFileStore`
- For index 0 (archive store):
  - Each archive ID maps to a decoded `RSArchive` (title=1, config=2, interface=3, media=4, etc.)
  - Files within are the named entries inside that `RSArchive`
- For indices 1–4 (models, animations, midi, maps):
  - These are flat file stores with no internal archive structure
  - Each file ID is treated as a single-file archive: `archiveId == fileId`, `fileId == 0`
- `readFile(indexId, archiveId)` shortcut reads the raw file from the store

## Consumer Changes

### App.kt

```kotlin
companion object {
    var cache: CacheSystem? = null

    fun openCache(path: Path) {
        cache?.close()
        cache = CacheSystemFactory.open(path)
        cache?.load()
    }
}
```

The old `val fs: RSFileSystem = RSFileSystem.getInstance()` is replaced. The `App.cache` field holds the currently active `CacheSystem` instance.

### LoadCacheEvent

```kotlin
class LoadCacheEvent(val cache: CacheSystem)
```

### ConfigExtension

Update `onSave` and `onLoad` to accept/use `CacheSystem` instead of directly calling `RSFileSystem.getInstance()`. The extension reads from the cache via the interface methods.

### Plugins

Plugins that currently reference `RSFileSystem` or `RSArchive` directly will need minor updates to go through the `CacheSystem` interface. For plugins that specifically need legacy `RSArchive` features (like named-hash file lookup), a utility method or cast can be provided.

## Migration Strategy

1. Add the interface and factory
2. Implement `LegacyCacheSystem` wrapping existing classes (no behavior change)
3. Implement `ModernCacheSystem` wrapping Displee
4. Update `App`, `LoadCacheEvent`, and `ConfigExtension` to use the interface
5. Update plugins one by one
6. Verify both formats load correctly

## Testing

This project currently has no tests. We introduce tests specifically for the new cache abstraction.

- Unit test: `CacheSystemFactory.open()` correctly detects format from a directory
- Unit test: `ModernCacheSystem` loads the cache at `/home/dev/prototypes/cache`, verify index/archive/file reads
- Unit test: Verify `CacheIndex` and `CacheArchive` return expected counts and data for the modern cache

Test infrastructure: add a `test` source set to the `fs` module with JUnit 5.

## Plugin Compatibility

Existing plugins under `plugins/` (itemdef, objdef, npc, sprite, texture, varbit, archive) must continue to work. These plugins currently depend on:
- `RSFileSystem.getInstance()` — replaced by `CacheSystem` accessed via `App`
- `RSArchive` — used directly in `ConfigExtension.onLoad/onSave`
- `RSFileStore` constants (e.g., `ARCHIVE_FILE_STORE`)

Strategy:
- `ConfigExtension` is updated to use `CacheSystem` interface methods
- Plugins that extend `ConfigExtension` should require no changes (they override `decode`/`encode`, not file system access)
- Any plugin that directly references `RSFileSystem.getInstance()` is updated to use the new `App.cache` accessor
- Verify all plugins compile and load after the migration
