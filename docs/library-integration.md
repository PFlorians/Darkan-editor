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

Plugins never import RuneLite or OpenRS2 directly. The `fs/codec` package wraps both libraries and exposes `BufferedImage`-based APIs.

## Repositories

```groovy
repositories {
    mavenCentral()                                               // Displee
    maven { url 'https://repo.openrs2.org/repository/openrs2' }  // OpenRS2
    maven { url 'https://repo.runelite.net' }                    // RuneLite
}
```

## Key Classes

### ModernSpriteCodec (`fs/src/main/java/darkan/editor/fs/codec/`)

- `decode(byte[] data): List<ModernSpriteFrame>` — Uses RuneLite's `SpriteLoader` to decode modern sprite archives (Index 8) into frame images
- `encode(List<BufferedImage> frames): byte[]` — Uses OpenRS2's `Sprite.fromImages()` + `write()` to encode images back to cache binary format

### ModernSpriteFrame

Data record holding: `id`, `frame`, `width`, `height`, `offsetX`, `offsetY`, `maxWidth`, `maxHeight`, `image` (BufferedImage)

### ModernTextureCodec (`fs/src/main/java/darkan/editor/fs/codec/`)

- `decode(int id, byte[] data): ModernTextureDef` — Uses RuneLite's `TextureLoader` to decode texture metadata from Index 9
- `encode(ModernTextureDef def): byte[]` — Manual encoding (format is a simple fixed struct)
- `renderTexture(def, spriteResolver): BufferedImage` — Resolves sprite ID reference to a viewable image

### ModernTextureDef

Data record holding: `id`, `spriteIds` (int[]), `animationSpeed`, `animationDirection`, `field1778`, `missingColor`

## Modern Sprite Format (Index 8)

Each archive in Index 8 is a sprite group containing one or more frames. Binary layout (read from end of file backwards):

1. Last 2 bytes: frame count
2. Max width (2), max height (2), palette size - 1 (1)
3. Per-frame metadata: offsetX (2), offsetY (2), width (2), height (2)
4. Palette: `(size - 1) * 3` bytes of RGB values
5. Pixel data (from offset 0): per-frame flag byte (bit 0 = vertical order, bit 1 = alpha present), then palette indices, then optional alpha bytes

## Modern Texture Format (Index 9)

All textures live in archive 0 of Index 9. Each file is a texture definition (rev233 format):

| Offset | Size | Field |
|--------|------|-------|
| 0 | 2 | Sprite file ID (references Index 8) |
| 2 | 2 | Missing/fallback color |
| 4 | 1 | field1778 flag (boolean) |
| 5 | 1 | Animation direction |
| 6 | 1 | Animation speed |

Textures are rendered by resolving their sprite reference from Index 8. Replacing a texture means replacing its underlying sprite.

## Future: Map Support

Map regions (Index 5) will require:

- **XTEA keys** for decryption — available from OpenRS2's public archive API (`https://archive.openrs2.org/keys`)
- **Terrain decoder** — RuneLite's `Region` class handles terrain tiles (heights, underlays, overlays, flags)
- **Object placement parser** — location data within map regions
- **Map plugin** — a new plugin with a 2D tile-based editor UI

The codec infrastructure established here (RuneLite decode + manual/OpenRS2 encode, wrapped in `fs/codec`) provides the pattern for implementing map codecs.
