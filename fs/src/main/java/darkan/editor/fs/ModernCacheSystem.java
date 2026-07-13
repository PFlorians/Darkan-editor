package darkan.editor.fs;

import com.displee.cache.CacheLibrary;
import com.displee.cache.index.Index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public final class ModernCacheSystem implements CacheSystem {

    private final Path root;
    private CacheLibrary library;
    private boolean loaded;

    public ModernCacheSystem(Path root) {
        this.root = root;
    }

    @Override
    public boolean load() {
        try {
            library = CacheLibrary.Companion.create(root.toString());
            loaded = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void close() throws IOException {
        if (library != null) {
            library.close();
            loaded = false;
        }
    }

    @Override
    public Path getRoot() {
        return root;
    }

    @Override
    public int getIndexCount() {
        if (library == null) {
            return 0;
        }
        return library.getIndices().length;
    }

    @Override
    public CacheIndex getIndex(int indexId) {
        if (library == null) {
            return null;
        }
        Index index = library.index(indexId);
        if (index == null) {
            return null;
        }
        return new ModernCacheIndex(index);
    }

    @Override
    public ByteBuffer readFile(int indexId, int archiveId, int fileId) {
        if (library == null) {
            return null;
        }
        byte[] data = library.data(indexId, archiveId, fileId);
        if (data == null) {
            return null;
        }
        return ByteBuffer.wrap(data);
    }

    @Override
    public ByteBuffer readFile(int indexId, int archiveId) {
        if (library == null) {
            return null;
        }
        byte[] data = library.data(indexId, archiveId);
        if (data == null) {
            return null;
        }
        return ByteBuffer.wrap(data);
    }

    @Override
    public boolean writeFile(int indexId, int archiveId, int fileId, byte[] data) {
        if (library == null) {
            return false;
        }
        try {
            library.put(indexId, archiveId, fileId, data);
            library.index(indexId).update();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean writeFile(int indexId, int archiveId, byte[] data) {
        if (library == null) {
            return false;
        }
        try {
            library.put(indexId, archiveId, data);
            library.index(indexId).update();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public CacheFormat getFormat() {
        return CacheFormat.MODERN;
    }

    public CacheLibrary getLibrary() {
        return library;
    }
}
