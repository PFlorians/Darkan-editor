package darkan.editor.fs.codec;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModernSpriteCodecTest {

    @Test
    void encodeAndDecodeRoundTrip() {
        BufferedImage original = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                original.setRGB(x, y, 0xFFFF0000); // solid red, fully opaque
            }
        }

        byte[] encoded = ModernSpriteCodec.encode(List.of(original));
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        List<ModernSpriteFrame> frames = ModernSpriteCodec.decode(encoded);
        assertEquals(1, frames.size());

        ModernSpriteFrame frame = frames.get(0);
        assertEquals(4, frame.width());
        assertEquals(4, frame.height());

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                int pixel = frame.image().getRGB(x, y);
                assertEquals(0xFFFF0000, pixel, "Pixel mismatch at " + x + "," + y);
            }
        }
    }

    @Test
    void encodeMultipleFrames() {
        BufferedImage frame1 = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        BufferedImage frame2 = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                frame1.setRGB(x, y, 0xFF00FF00); // green
                frame2.setRGB(x, y, 0xFF0000FF); // blue
            }
        }

        byte[] encoded = ModernSpriteCodec.encode(List.of(frame1, frame2));
        assertNotNull(encoded);

        List<ModernSpriteFrame> decoded = ModernSpriteCodec.decode(encoded);
        assertEquals(2, decoded.size());
        assertEquals(8, decoded.get(0).width());
        assertEquals(8, decoded.get(1).width());
    }
}
