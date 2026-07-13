package darkan.editor.fs;

public interface CacheIndex {

    int getId();

    int getArchiveCount();

    CacheArchive getArchive(int archiveId);

    int[] getArchiveIds();
}
