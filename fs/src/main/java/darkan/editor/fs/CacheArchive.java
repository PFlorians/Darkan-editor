package darkan.editor.fs;

import java.nio.ByteBuffer;

public interface CacheArchive {

    int getId();

    int getFileCount();

    int[] getFileIds();

    ByteBuffer readFile(int fileId);

    boolean writeFile(int fileId, byte[] data);
}
