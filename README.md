# Darkan Editor

A desktop cache editor for RuneScape 2 / Old School RuneScape game caches. Supports both legacy (revision 317) and modern (post-317 OSRS) cache formats through a unified plugin architecture.

Built with Java 21, Kotlin, JavaFX 21, and [Displee's rs-cache-library](https://github.com/Displee/rs-cache-library).

## Features

- Dual-format cache support with automatic format detection
- Plugin-based architecture for editing different cache data types
- NPC, Item, and Object definition editors with full decode/encode (read and write)
- Sprite and texture viewers
- 3D model viewer with custom rasterizer
- Generic archive browser for raw cache exploration
- Varbit configuration editor

## Plugins

| Plugin | Description | Write Support |
|--------|-------------|---------------|
| Archive | Browse and manage raw cache archives and indices | Legacy only |
| Item Definitions | Edit item names, models, options, colours | Yes |
| NPC Definitions | Edit NPC names, models, combat, animations | Yes |
| Object Definitions | Edit object names, models, options | Yes |
| Sprite Viewer | View vanilla 317 sprites | Read only |
| Texture Viewer | View cache textures | Read only (legacy only) |
| Model Viewer | 3D model rendering | Read only |
| Varbit Editor | Edit varbit configurations | Yes |

## Requirements

- Java 21+
- Gradle 9.5+ (included via wrapper)

## Building

```sh
./gradlew build
```

This compiles all modules and plugins. Plugin JARs are output to `plugins-runtime/`.

## Running

```sh
./gradlew run
```

Or use the default task:

```sh
./gradlew
```

The editor will open and prompt you to select a cache directory. It accepts any directory containing either:
- `main_file_cache.dat` (legacy 317 format)
- `main_file_cache.dat2` (modern OSRS format)

## Project Structure

```
darkan-editor/
├── fs/              Core file system and cache abstraction
├── gui/             JavaFX application and UI controllers
├── io/              RSBuffer I/O utilities
├── util/            Compression and helper utilities
├── plugin/          Plugin framework (interfaces, manager, event bus)
├── shared/          Shared models and JavaFX base types
├── plugins/         Plugin source code (each compiles to a JAR)
├── plugins-runtime/ Compiled plugin JARs (loaded at startup)
└── docs/            Documentation
```

See [docs/architecture.md](docs/architecture.md) for detailed architecture documentation.

## Creating Plugins

See [docs/developing-plugins.md](docs/developing-plugins.md) for a guide on writing new plugins.

## License

ISC License - Copyright (c) 2018-2019 Nshusa
