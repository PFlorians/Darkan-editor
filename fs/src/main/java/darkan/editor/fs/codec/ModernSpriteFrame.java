package darkan.editor.fs.codec;

import java.awt.image.BufferedImage;

public record ModernSpriteFrame(
    int id,
    int frame,
    int width,
    int height,
    int offsetX,
    int offsetY,
    int maxWidth,
    int maxHeight,
    BufferedImage image
) {}
