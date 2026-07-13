package darkan.editor.fs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModernCacheSystemTest {

    private static final Path CACHE_PATH = Path.of("/home/dev/prototypes/cache");

    private ModernCacheSystem cache;

    @BeforeEach
    void setUp() {
        cache = new ModernCacheSystem(CACHE_PATH);
        assertTrue(cache.load(), "Failed to load modern cache at " + CACHE_PATH);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (cache != null) {
            cache.close();
        }
    }

    @Test
    void loadReportsCorrectFormat() {
        assertEquals(CacheFormat.MODERN, cache.getFormat());
    }

    @Test
    void loadReportsLoaded() {
        assertTrue(cache.isLoaded());
    }

    @Test
    void getIndexCountIsPositive() {
        assertTrue(cache.getIndexCount() > 0, "Expected at least one index");
    }

    @Test
    void getIndexReturnsNonNull() {
        CacheIndex index = cache.getIndex(0);
        assertNotNull(index, "Index 0 should exist");
        assertEquals(0, index.getId());
    }

    @Test
    void indexHasArchives() {
        CacheIndex index = cache.getIndex(0);
        assertNotNull(index);
        assertTrue(index.getArchiveCount() > 0, "Index 0 should have archives");
    }

    @Test
    void readFileReturnsData() {
        CacheIndex index = cache.getIndex(0);
        assertNotNull(index);
        int[] archiveIds = index.getArchiveIds();
        assertTrue(archiveIds.length > 0);

        CacheArchive archive = index.getArchive(archiveIds[0]);
        assertNotNull(archive);

        int[] fileIds = archive.getFileIds();
        if (fileIds.length > 0) {
            ByteBuffer data = archive.readFile(fileIds[0]);
            assertNotNull(data);
            assertTrue(data.remaining() > 0);
        }
    }

    @Test
    void readFileViaSystemShortcut() {
        CacheIndex index = cache.getIndex(0);
        assertNotNull(index);
        int[] archiveIds = index.getArchiveIds();
        assertTrue(archiveIds.length > 0);

        ByteBuffer data = cache.readFile(0, archiveIds[0]);
        assertNotNull(data);
    }

    @Test
    void closeMarksUnloaded() throws Exception {
        cache.close();
        assertFalse(cache.isLoaded());
    }
}
