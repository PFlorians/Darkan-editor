package darkan.editor.fs;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CacheSystemFactory {

    private CacheSystemFactory() {}

    public static CacheSystem open(Path path) {
        if (Files.exists(path.resolve(CacheConstants.MODERN_DATA_FILE))) {
            return new ModernCacheSystem(path);
        } else if (Files.exists(path.resolve(CacheConstants.LEGACY_DATA_FILE))) {
            return new LegacyCacheSystem(path);
        }
        throw new IllegalArgumentException("No recognized cache format at: " + path);
    }
}
