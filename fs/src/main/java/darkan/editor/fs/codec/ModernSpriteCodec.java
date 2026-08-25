package darkan.editor.fs.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.runelite.cache.definitions.SpriteDefinition;
import net.runelite.cache.definitions.loaders.SpriteLoader;
import org.openrs2.cache.sprite.Sprite;

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
            if (srcPixels != null && srcPixels.length == destPixels.length) {
                System.arraycopy(srcPixels, 0, destPixels, 0, srcPixels.length);
            }

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

    public static byte[] encode(List<BufferedImage> frames) {
        Sprite sprite;
        if (frames.size() == 1) {
            sprite = Sprite.Companion.fromImage(frames.get(0));
        } else {
            sprite = Sprite.Companion.fromImages(frames);
        }

        ByteBuf buf = Unpooled.buffer();
        try {
            sprite.write(buf);
            byte[] result = new byte[buf.readableBytes()];
            buf.readBytes(result);
            return result;
        } finally {
            buf.release();
        }
    }
}
