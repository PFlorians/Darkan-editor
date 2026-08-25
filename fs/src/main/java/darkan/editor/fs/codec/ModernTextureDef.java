package darkan.editor.fs.codec;

public record ModernTextureDef(
    int id,
    int[] spriteIds,
    int animationSpeed,
    int animationDirection,
    boolean field1778,
    int missingColor
) {}
