package darkan.editor.fs.codec;

import net.runelite.cache.definitions.TextureDefinition;
import net.runelite.cache.definitions.loaders.TextureLoader;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.function.Function;

public final class ModernTextureCodec {

    private ModernTextureCodec() {}

    public static ModernTextureDef decode(int id, byte[] data) {
        TextureLoader loader = new TextureLoader();
        TextureDefinition def = loader.load(id, data);

        return new ModernTextureDef(
            id,
            def.getFileIds(),
            def.getAnimationSpeed(),
            def.getAnimationDirection(),
            def.isField1778(),
            def.getMissingColor()
        );
    }

    public static byte[] encode(ModernTextureDef def) {
        // Modern texture format (rev233):
        // - unsigned short: sprite file ID
        // - unsigned short: missing color
        // - unsigned byte: field1778 (boolean)
        // - unsigned byte: animation direction
        // - unsigned byte: animation speed
        ByteBuffer buf = ByteBuffer.allocate(7);
        buf.putShort((short) (def.spriteIds().length > 0 ? def.spriteIds()[0] : 0));
        buf.putShort((short) def.missingColor());
        buf.put((byte) (def.field1778() ? 1 : 0));
        buf.put((byte) def.animationDirection());
        buf.put((byte) def.animationSpeed());
        return buf.array();
    }

    public static BufferedImage renderTexture(ModernTextureDef def,
                                              Function<Integer, BufferedImage> spriteResolver) {
        if (def.spriteIds() == null || def.spriteIds().length == 0) {
            return new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        }
        BufferedImage sprite = spriteResolver.apply(def.spriteIds()[0]);
        if (sprite == null) {
            return new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        }
        return sprite;
    }
}
