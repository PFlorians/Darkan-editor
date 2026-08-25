package darkan.editor.fs.codec;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ModernTextureCodecTest {

    @Test
    void encodeAndDecodeRoundTrip() {
        ModernTextureDef original = new ModernTextureDef(
            42, new int[]{100}, 0, 0, false, 0
        );

        byte[] encoded = ModernTextureCodec.encode(original);
        assertNotNull(encoded);
        assertEquals(7, encoded.length);

        ModernTextureDef decoded = ModernTextureCodec.decode(42, encoded);
        assertEquals(42, decoded.id());
        assertArrayEquals(new int[]{100}, decoded.spriteIds());
        assertEquals(0, decoded.animationSpeed());
        assertEquals(0, decoded.animationDirection());
        assertFalse(decoded.field1778());
    }

    @Test
    void renderTextureResolvesSprite() {
        ModernTextureDef def = new ModernTextureDef(
            1, new int[]{50}, 0, 0, false, 0
        );

        BufferedImage testSprite = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

        BufferedImage result = ModernTextureCodec.renderTexture(def, spriteId -> {
            assertEquals(50, spriteId);
            return testSprite;
        });

        assertSame(testSprite, result);
    }

    @Test
    void renderTextureWithNoSpritesReturnsFallback() {
        ModernTextureDef def = new ModernTextureDef(
            1, new int[]{}, 0, 0, false, 0
        );

        BufferedImage result = ModernTextureCodec.renderTexture(def, spriteId -> null);
        assertEquals(64, result.getWidth());
        assertEquals(64, result.getHeight());
    }
}
