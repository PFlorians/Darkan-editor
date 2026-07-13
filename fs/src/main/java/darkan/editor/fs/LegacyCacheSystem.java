package darkan.editor.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public final class LegacyCacheSystem implements CacheSystem {

    private final RSFileSystem fileSystem;
    private final Path root;

    public LegacyCacheSystem(Path root) {
        this.root = root;
        this.fileSystem = new RSFileSystem();
        this.fileSystem.setRoot(root);
    }

    @Override
    public boolean load() {
        return fileSystem.load();
    }

    @Override
    public boolean isLoaded() {
        return fileSystem.isLoaded();
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }

    @Override
    public Path getRoot() {
        return root;
    }

    @Override
    public int getIndexCount() {
        return fileSystem.getStoreCount();
    }

    @Override
    public CacheIndex getIndex(int indexId) {
        RSFileStore store = fileSystem.getStore(indexId);
        if (store == null) {
            return null;
        }
        return new LegacyCacheIndex(indexId, store);
    }

    @Override
    public ByteBuffer readFile(int indexId, int archiveId, int fileId) {
        CacheIndex index = getIndex(indexId);
        if (index == null) {
            return null;
        }
        CacheArchive archive = index.getArchive(archiveId);
        if (archive == null) {
            return null;
        }
        return archive.readFile(fileId);
    }

    @Override
    public ByteBuffer readFile(int indexId, int archiveId) {
        RSFileStore store = fileSystem.getStore(indexId);
        if (store == null) {
            return null;
        }
        return store.readFile(archiveId);
    }

    @Override
    public boolean writeFile(int indexId, int archiveId, int fileId, byte[] data) {
        CacheIndex index = getIndex(indexId);
        if (index == null) {
            return false;
        }
        CacheArchive archive = index.getArchive(archiveId);
        if (archive == null) {
            return false;
        }
        return archive.writeFile(fileId, data);
    }

    @Override
    public boolean writeFile(int indexId, int archiveId, byte[] data) {
        RSFileStore store = fileSystem.getStore(indexId);
        if (store == null) {
            return false;
        }
        return store.writeFile(archiveId, data);
    }

    @Override
    public CacheFormat getFormat() {
        return CacheFormat.LEGACY_317;
    }

    public RSFileSystem getFileSystem() {
        return fileSystem;
    }
}
