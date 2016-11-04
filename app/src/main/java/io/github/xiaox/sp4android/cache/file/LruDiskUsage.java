package io.github.xiaox.sp4android.cache.file;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author X
 * @version 0.1.0
 */

public abstract class LruDiskUsage implements DiskUsage {

    private final ExecutorService workThread = Executors.newSingleThreadExecutor();

    @Override
    public void touch(File file) throws IOException {
        workThread.submit(new TouchRunnable(file));
    }

    protected abstract boolean accept(File file, long totalSize, int totalCount);


    private final class TouchRunnable implements Runnable {

        private final File file;

        TouchRunnable(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            try {
                touchInBackground(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void touchInBackground(File file) throws IOException {
        Files.setLastModifiedNow(file);
        trim(Files.getLruListFiles(file.getParentFile()));
    }

    private void trim(List<File> files) {
        long totalSize = countTotalSize(files);
        int totalCount = files.size();
        for (File file : files) {
            boolean accepted = accept(file, totalSize, totalCount);
            if (!accepted) {
                long fileSize = file.length();
                boolean deleted = file.delete();
                if (deleted) {
                    totalCount--;
                    totalSize -= fileSize;
                }
            }
        }
    }


    private long countTotalSize(List<File> files) {
        long totalSize = 0;
        for (File file : files) {
            totalSize += file.length();
        }
        return totalSize;
    }


}
