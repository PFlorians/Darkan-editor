package darkan.editor.fs;

/**
 * Global holder for the currently active CacheSystem instance.
 * This allows modules that depend on fs (like plugin) to access the cache
 * without depending on the gui module.
 */
public final class CacheSystemHolder {

    private static CacheSystem instance;

    private CacheSystemHolder() {}

    public static CacheSystem get() {
        return instance;
    }

    public static void set(CacheSystem cache) {
        instance = cache;
    }
}
