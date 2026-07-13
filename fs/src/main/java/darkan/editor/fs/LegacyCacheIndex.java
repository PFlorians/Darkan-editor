package darkan.editor.fs;

import java.nio.ByteBuffer;

public final class LegacyCacheIndex implements CacheIndex {

    private final int id;
    private final RSFileStore store;

    LegacyCacheIndex(int id, RSFileStore store) {
        this.id = id;
        this.store = store;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getArchiveCount() {
        return store.getFileCount();
    }

    @Override
    public CacheArchive getArchive(int archiveId) {
        if (id == RSFileStore.ARCHIVE_FILE_STORE) {
            ByteBuffer data = store.readFile(archiveId);
            if (data == null) {
                return null;
            }
            try {
                RSArchive archive = RSArchive.decode(data);
                return new LegacyCacheArchive(archiveId, archive);
            } catch (Exception e) {
                return null;
            }
        }
        ByteBuffer data = store.readFile(archiveId);
        return new LegacyCacheArchive(archiveId, data);
    }

    @Override
    public int[] getArchiveIds() {
        int count = store.getFileCount();
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            ids[i] = i;
        }
        return ids;
    }

    public RSFileStore getStore() {
        return store;
    }
}
