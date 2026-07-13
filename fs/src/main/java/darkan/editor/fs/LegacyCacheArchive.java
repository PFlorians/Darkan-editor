package darkan.editor.fs;

import java.nio.ByteBuffer;

public final class LegacyCacheArchive implements CacheArchive {

    private final int id;
    private final RSArchive archive;
    private final ByteBuffer rawData;

    LegacyCacheArchive(int id, RSArchive archive) {
        this.id = id;
        this.archive = archive;
        this.rawData = null;
    }

    LegacyCacheArchive(int id, ByteBuffer rawData) {
        this.id = id;
        this.archive = null;
        this.rawData = rawData;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getFileCount() {
        if (archive != null) {
            return archive.getEntryCount();
        }
        return rawData != null ? 1 : 0;
    }

    @Override
    public int[] getFileIds() {
        if (archive != null) {
            int count = archive.getEntryCount();
            int[] ids = new int[count];
            for (int i = 0; i < count; i++) {
                ids[i] = i;
            }
            return ids;
        }
        return rawData != null ? new int[]{0} : new int[0];
    }

    @Override
    public ByteBuffer readFile(int fileId) {
        if (archive != null) {
            try {
                RSArchive.ArchiveEntry entry = archive.getEntryAt(fileId);
                return ByteBuffer.wrap(entry.getData());
            } catch (Exception e) {
                return null;
            }
        }
        if (rawData != null && fileId == 0) {
            return rawData.duplicate();
        }
        return null;
    }

    @Override
    public boolean writeFile(int fileId, byte[] data) {
        if (archive != null) {
            try {
                return archive.writeFile(fileId, data);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public RSArchive getRSArchive() {
        return archive;
    }
}
