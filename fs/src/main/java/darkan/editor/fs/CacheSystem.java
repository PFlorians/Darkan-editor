package darkan.editor.fs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public interface CacheSystem extends Closeable {

    boolean load();

    boolean isLoaded();

    void close() throws IOException;

    Path getRoot();

    int getIndexCount();

    CacheIndex getIndex(int indexId);

    ByteBuffer readFile(int indexId, int archiveId, int fileId);

    ByteBuffer readFile(int indexId, int archiveId);

    boolean writeFile(int indexId, int archiveId, int fileId, byte[] data);

    boolean writeFile(int indexId, int archiveId, byte[] data);

    CacheFormat getFormat();
}
