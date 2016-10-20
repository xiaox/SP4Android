package io.github.xiaox.sp4android.cache;

import java.io.File;

/**
 * @author X
 * @version V0.1.0
 */
public interface CacheListener {
    void onCacheAvailable(File cacheFile, String url, int percentAvailable);
}
