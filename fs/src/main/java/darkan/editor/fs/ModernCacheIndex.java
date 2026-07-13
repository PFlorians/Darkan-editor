package darkan.editor.fs;

import com.displee.cache.index.Index;
import com.displee.cache.index.archive.Archive;

public final class ModernCacheIndex implements CacheIndex {

    private final Index index;

    ModernCacheIndex(Index index) {
        this.index = index;
    }

    @Override
    public int getId() {
        return index.getId();
    }

    @Override
    public int getArchiveCount() {
        return index.archiveIds().length;
    }

    @Override
    public CacheArchive getArchive(int archiveId) {
        Archive archive = index.archive(archiveId);
        if (archive == null) {
            return null;
        }
        return new ModernCacheArchive(archive);
    }

    @Override
    public int[] getArchiveIds() {
        return index.archiveIds();
    }

    public Index getDispleeIndex() {
        return index;
    }
}
