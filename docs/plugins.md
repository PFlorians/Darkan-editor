# Plugins

Each plugin is a self-contained Gradle subproject under `plugins/`. At build time, each compiles to a JAR in `plugins-runtime/`, which the editor discovers and loads at startup.

## Archive Plugin

**Purpose:** Generic browser for raw cache archives and indices. Allows navigating the full cache structure, viewing file metadata, and performing low-level operations.

**Capabilities:**
- Browse all indices and archives in both legacy and modern caches
- View archive entry names and sizes
- Export individual files or entire archives
- Add, replace, and pack entries (legacy format only)
- Displays human-readable index names for modern caches (e.g., Index 18 = "NPC Definitions")

**Limitations:** Add/replace/pack operations are not yet wired up for modern caches through the UI, though the underlying API supports it.

## Item Definition Plugin

**Purpose:** Edit item definitions (names, models, options, colours, stats).

**Cache location:**
- Legacy: `config` archive, `obj.dat` / `obj.idx`
- Modern: Index 19

**Capabilities:** Full decode and encode. Edit any field and save back to cache.

## NPC Definition Plugin

**Purpose:** Edit NPC definitions (names, models, combat levels, animations, actions, colours).

**Cache location:**
- Legacy: `config` archive, `npc.dat` / `npc.idx`
- Modern: Index 18

**Capabilities:** Full decode and encode. Supports morphisms, head models, colour/texture replacements, and all standard NPC fields.

## Object Definition Plugin

**Purpose:** Edit object (loc) definitions (names, models, options, dimensions).

**Cache location:**
- Legacy: `config` archive, `loc.dat` / `loc.idx`
- Modern: Index 6

**Capabilities:** Full decode and encode.

## Sprite Plugin

**Purpose:** View sprites from the legacy 317 cache format.

**Cache location:** Legacy archive file store

**Capabilities:** Renders sprites as images with scaling support via imgscalr. Read-only.

## Texture Plugin

**Purpose:** View textures from the cache.

**Cache location:** Legacy format only (texture archive)

**Capabilities:** Renders textures as viewable images. Read-only.

**Limitations:** Does not currently work with modern cache textures. Modern caches store textures differently (Index 9) and would need a separate decoder.

## Model Viewer Plugin

**Purpose:** Render 3D models from the cache.

**Capabilities:**
- Custom software rasterizer (no GPU dependency)
- Matrix4 transformations, vertex/triangle rendering
- Renders models loaded from cache binary data

**Limitations:** Read-only viewing. No model editing.

## Varbit Plugin

**Purpose:** Edit varbit (variable bit) configurations used by the game engine for player state.

**Cache location:**
- Legacy: `config` archive, `varbit.dat` / `varbit.idx`
- Modern: Index 2

**Capabilities:** Full decode and encode. Edit high bit, low bit, and setting ID.
