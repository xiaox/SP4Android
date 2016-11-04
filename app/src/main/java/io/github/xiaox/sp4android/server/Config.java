package io.github.xiaox.sp4android.server;

import java.io.File;

import io.github.xiaox.sp4android.cache.file.DiskUsage;
import io.github.xiaox.sp4android.cache.file.FileNameGenerator;
import io.github.xiaox.sp4android.sourcestorage.SourceInfoStorage;

/**
 * @author X
 * @version 0.1.0
 */

public class Config {

    public final File cacheRoot;
    public final FileNameGenerator fileNameGenerator;
    public final DiskUsage diskUsage;
    public final SourceInfoStorage sourceInfoStorage;

    Config(File cacheRoot, FileNameGenerator fileNameGenerator, DiskUsage diskUsage, SourceInfoStorage sourceInfoStorage) {
        this.cacheRoot = cacheRoot;
        this.fileNameGenerator = fileNameGenerator;
        this.diskUsage = diskUsage;
        this.sourceInfoStorage = sourceInfoStorage;
    }

    File generateCacheFile(String url) {
        String name = fileNameGenerator.generate(url);
        return new File(cacheRoot, name);
    }


}
