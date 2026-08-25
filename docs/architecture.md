# Architecture

## Overview

Darkan Editor is a multi-module Gradle project. Each module has a single responsibility, and plugins extend the editor without modifying core code.

```
┌─────────────────────────────────────────────────┐
│                   gui (App)                      │
│         JavaFX application, controllers         │
├──────────┬──────────┬───────────┬───────────────┤
│  plugin  │    fs    │    io     │     util      │
│framework │  cache   │ RSBuffer  │ compression   │
├──────────┴──────────┴───────────┴───────────────┤
│                   shared                        │
│            Common models, JavaFX base           │
└─────────────────────────────────────────────────┘

        ┌─────────────────────────────┐
        │     plugins-runtime/*.jar    │
        │  Loaded dynamically at boot  │
        └─────────────────────────────┘
```

## Modules

### fs (File System)

The cache abstraction layer. Provides a unified interface over two cache formats:

- **`CacheSystem`** - Top-level interface: load, read, write, close
- **`CacheIndex`** - Represents a cache index (archive group)
- **`CacheArchive`** - Represents an archive within an index
- **`CacheFormat`** - Enum: `LEGACY_317` or `MODERN`
- **`CacheSystemFactory`** - Auto-detects format from the cache directory

Implementations:
- `LegacyCacheSystem` / `LegacyCacheIndex` / `LegacyCacheArchive` - Wraps the built-in `RSFileSystem`, `RSFileStore`, `RSArchive` classes for revision 317 caches
- `ModernCacheSystem` / `ModernCacheIndex` / `ModernCacheArchive` - Wraps Displee's `CacheLibrary` for OSRS caches

The factory inspects the cache directory for `main_file_cache.dat2` (modern) or `main_file_cache.dat` (legacy) and returns the correct implementation.

Graphics classes (`RSSprite`, `RSTexture`, `RSModel`, `RSFont`, `RSRaster`, `RSRasterizer`) provide decoding for visual cache data.

#### fs/codec (Format Codecs)

Wraps external libraries for format-specific decoding and encoding of modern cache data:

- `ModernSpriteCodec` — Decode (RuneLite) and encode (OpenRS2) modern sprites (Index 8)
- `ModernSpriteFrame` — Data record for a decoded sprite frame
- `ModernTextureCodec` — Decode and encode modern texture definitions (Index 9)
- `ModernTextureDef` — Data record for texture metadata

Plugins use these codecs via `BufferedImage` — they never interact with RuneLite or OpenRS2 directly. See [library-integration.md](library-integration.md) for details on the hybrid library approach.

### gui

The JavaFX 21 application. Key classes:

- `App` - Application lifecycle, holds the global `CacheSystem` singleton
- `Launcher` - JVM entry point
- `StoreController` - Main scene, lists plugins, handles cache open/close
- `BaseController` - Shared controller logic for plugin scenes
- `Settings` - Persists user preferences (last cache path)
- `PluginManager` integration - Discovers and loads plugin JARs at startup

### io

Single class: `RSBuffer`. A wrapper over `ByteBuffer` with RuneScape-specific read/write methods (unsigned bytes, tri-bytes, strings, big-smart values, etc.).

### util

Compression utilities (bzip2, gzip) and helper functions for archive encoding.

### plugin

The plugin framework:

- **`IPlugin`** - Interface every plugin implements (provides FXML path, stylesheets, icon)
- **`PluginDescriptor`** - Annotation for metadata (name, authors, version)
- **`PluginManager`** - Scans `plugins-runtime/` for JARs, loads classes via isolated `PluginClassLoader`
- **`ConfigExtension`** - Base class for definition-based plugins (items, NPCs, objects, varbits). Handles decode/encode lifecycle, loading from both legacy archives and modern indexed files, and saving back.
- **Event Bus** - Guava `EventBus` for inter-plugin communication (e.g., `LoadCacheEvent`)

### shared

Shared JavaFX model classes (`KeyModel`, `NamedValueModel`, `ValueModel`) used by both the gui and plugins.

## Cache System Design

```
User selects directory
        │
        ▼
CacheSystemFactory.open(path)
        │
        ├── main_file_cache.dat2 exists? → ModernCacheSystem (Displee)
        │
        └── main_file_cache.dat exists?  → LegacyCacheSystem (built-in)
        │
        ▼
CacheSystem interface
        │
        ├── readFile(index, archive, file) → ByteBuffer
        ├── writeFile(index, archive, file, data) → boolean
        ├── getIndex(id) → CacheIndex
        └── getFormat() → CacheFormat
```

Both implementations support reading and writing. The `CacheSystemHolder` singleton broadcasts the active cache to plugins without requiring them to depend on the gui module.

## Data Flow for Definition Plugins

```
Cache file (binary) → RSBuffer → decode() → Java fields → reflection → UI table
                                                                           │
UI edit ← table cell edit ← ValueModel                                     │
   │                                                                       │
   ▼                                                                       │
Java fields ← mapToInstance() ← updated map ◄─────────────────────────────┘
   │
   ▼
encode() → RSBuffer → writeFile() → Cache
```

`ConfigExtension` automates this flow. Plugin authors only need to implement `decode()` and `encode()` to support a new definition type.

## Modern Cache Index Layout

Key indices in the modern (OSRS) cache format:

| Index | Content |
|-------|---------|
| 0 | Animations |
| 1 | Skeletons |
| 2 | Configs |
| 5 | Maps |
| 7 | Models |
| 8 | Sprites |
| 9 | Textures |
| 10 | Binary |
| 12 | Interfaces |
| 14 | Sound Effects |
| 18 | NPC Definitions |
| 19 | Item Definitions |
| 40 | World Map |
| 41 | World Map Labels |
| 42 | World Map Geography |
