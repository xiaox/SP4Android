package io.github.xiaox.sp4android.cache.file;

import java.io.File;
import java.io.IOException;

/**
 * @author X
 * @version V0.1.0
 */
public interface DiskUsage {

    void touch(File file) throws IOException;
}
