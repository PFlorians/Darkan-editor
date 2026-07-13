package darkan.editor.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CacheSystemFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    void openDetectsModernFormat() throws IOException {
        Files.createFile(tempDir.resolve(CacheConstants.MODERN_DATA_FILE));
        Files.createFile(tempDir.resolve(CacheConstants.INDEX_FILE_PREFIX + "0"));
        Files.createFile(tempDir.resolve(CacheConstants.INDEX_FILE_PREFIX + "255"));

        CacheSystem cache = CacheSystemFactory.open(tempDir);
        assertNotNull(cache);
        assertEquals(CacheFormat.MODERN, cache.getFormat());
    }

    @Test
    void openDetectsLegacyFormat() throws IOException {
        Files.createFile(tempDir.resolve(CacheConstants.LEGACY_DATA_FILE));
        Files.createFile(tempDir.resolve(CacheConstants.INDEX_FILE_PREFIX + "0"));

        CacheSystem cache = CacheSystemFactory.open(tempDir);
        assertNotNull(cache);
        assertEquals(CacheFormat.LEGACY_317, cache.getFormat());
    }

    @Test
    void openPrefersModernWhenBothExist() throws IOException {
        Files.createFile(tempDir.resolve(CacheConstants.MODERN_DATA_FILE));
        Files.createFile(tempDir.resolve(CacheConstants.LEGACY_DATA_FILE));
        Files.createFile(tempDir.resolve(CacheConstants.INDEX_FILE_PREFIX + "0"));

        CacheSystem cache = CacheSystemFactory.open(tempDir);
        assertEquals(CacheFormat.MODERN, cache.getFormat());
    }

    @Test
    void openThrowsForUnrecognizedFormat() {
        assertThrows(IllegalArgumentException.class, () -> CacheSystemFactory.open(tempDir));
    }
}
