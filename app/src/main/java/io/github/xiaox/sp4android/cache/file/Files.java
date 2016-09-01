package io.github.xiaox.sp4android.cache.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author X
 * @version V0.1.0
 */
class Files {

    static void makeDir(File directory) throws IOException {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IOException("File " + directory + " is not directory!");
            }
        } else {
            boolean isCreated = directory.mkdirs();
            if (!isCreated) {
                throw new IOException(String.format("Directory %s can't be created", directory.getAbsolutePath()));
            }
        }
    }

    static List<File> getLruListFiles(File directory) {
        List<File> result;
        File[] files = directory.listFiles();
        if (files != null) {
            result = Arrays.asList(files);
            Collections.sort(result, new LastModifiedComparator());
        } else {
            result = new LinkedList<>();
        }
        return result;
    }

    static void setLastModifiedNow(File file) throws IOException {
        if (file.exists()) {
            long now = System.currentTimeMillis();
            boolean modified = file.setLastModified(now);
            if (!modified) {
                modify(file);
                if (file.lastModified() < now) {
                    throw new IOException("Error set last modified date to " + file);
                }
            }
        }
    }

    static void modify(File file) throws IOException {
        long size = file.length();
        if (size == 0) {
            recreateZeroSizeFile(file);
            return;
        }

        RandomAccessFile accessFile = new RandomAccessFile(file, "rwd");
        accessFile.seek(size - 1);
        byte lastByte = accessFile.readByte();
        accessFile.seek(size - 1);
        accessFile.write(lastByte);
        accessFile.close();
    }

    private static void recreateZeroSizeFile(File file) throws IOException {
        if (!file.delete() || !file.createNewFile()) {
            throw new IOException("Error recreate zero-size file " + file);
        }
    }

    private static final class LastModifiedComparator implements Comparator<File> {

        @Override
        public int compare(File lhs, File rhs) {
            return compareLong(lhs.lastModified(), rhs.lastModified());
        }

        private int compareLong(long first, long secod) {
            return (first < secod) ? -1 : ((first == secod) ? 0 : 1);
        }
    }
}
