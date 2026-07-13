package darkan.editor.fs;

import com.displee.cache.index.archive.Archive;
import com.displee.cache.index.archive.file.File;

import java.nio.ByteBuffer;

public final class ModernCacheArchive implements CacheArchive {

    private final Archive archive;

    ModernCacheArchive(Archive archive) {
        this.archive = archive;
    }

    @Override
    public int getId() {
        return archive.getId();
    }

    @Override
    public int getFileCount() {
        return archive.fileIds().length;
    }

    @Override
    public int[] getFileIds() {
        return archive.fileIds();
    }

    @Override
    public ByteBuffer readFile(int fileId) {
        File file = archive.file(fileId);
        if (file == null) {
            return null;
        }
        byte[] data = file.getData();
        if (data == null) {
            return null;
        }
        return ByteBuffer.wrap(data);
    }

    @Override
    public boolean writeFile(int fileId, byte[] data) {
        archive.add(fileId, data);
        return true;
    }

    public Archive getDispleeArchive() {
        return archive;
    }
}
