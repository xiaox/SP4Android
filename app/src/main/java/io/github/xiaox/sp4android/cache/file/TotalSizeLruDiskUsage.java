package io.github.xiaox.sp4android.cache.file;

import java.io.File;

/**
 * @author X
 * @version 0.1.0
 */

public class TotalSizeLruDiskUsage extends LruDiskUsage {

    private final long maxSize;

    public TotalSizeLruDiskUsage(long maxSize) {
        if (maxSize <= 0) {
            maxSize = 128 * 1024 * 1024;
        }
        this.maxSize = maxSize;
    }

    @Override
    protected boolean accept(File file, long totalSize, int totalCount) {
        return totalSize < maxSize;
    }
}
