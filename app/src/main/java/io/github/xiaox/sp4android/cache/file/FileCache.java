package io.github.xiaox.sp4android.cache.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import io.github.xiaox.sp4android.cache.Cache;
import io.github.xiaox.sp4android.exception.ProxyCacheException;

/**
 * @author X
 * @version V0.1.0
 */
public class FileCache implements Cache {

    private static final String TEMP_POSTFIX = ".download";

    private final DiskUsage diskUsage;
    public File file;
    private RandomAccessFile dataFile;

    public FileCache(File file) throws ProxyCacheException {
        this(file, new UnlimitedDiskUsage());
    }

    public FileCache(File file, DiskUsage diskUsage) throws ProxyCacheException {
        if (diskUsage == null) {
            throw new NullPointerException();
        }
        this.diskUsage = diskUsage;
        File directory = file.getParentFile();
        try {
            Files.makeDir(directory);

            boolean completed = file.exists();
            this.file = completed ? file : new File(file.getParent(), file.getName() + TEMP_POSTFIX);
            this.dataFile = new RandomAccessFile(this.file, completed ? "r" : "rw");
        } catch (IOException e) {
            throw new ProxyCacheException("Error using file " + file + " as disc cache", e);
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws ProxyCacheException {
        try {
            dataFile.seek(offset);
            return dataFile.read(buffer, 0, length);
        } catch (IOException e) {
            String format = "Error reading %d bytes with offset %d from file[%d bytes] to buffer[%d bytes]";
            throw new ProxyCacheException(String.format(format, length, offset, available(), buffer.length), e);
        }
    }

    @Override
    public void append(byte[] data, int length) throws ProxyCacheException {
        if (isCompleted()) {
            throw new ProxyCacheException("Error append cache: cache file " + file + " is completed!");
        }
        try {
            dataFile.seek(available());
            dataFile.write(data, 0, length);
        } catch (IOException e) {
            String format = "Error writing %d bytes to %s from buffer with size %d";
            throw new ProxyCacheException(String.format(format, length, dataFile, data.length), e);
        }

    }

    @Override
    public void close() throws ProxyCacheException {
        try {
            dataFile.close();
            diskUsage.touch(file);
        } catch (IOException e) {
            throw new ProxyCacheException("Error closing file " + file, e);
        }
    }

    @Override
    public int available() throws ProxyCacheException {
        try {
            return (int) dataFile.length();
        } catch (IOException e) {
            throw new ProxyCacheException("Error reading length of file " + file, e);
        }
    }

    @Override
    public void complete() throws ProxyCacheException {
        if (isCompleted()) return;

        close();

        String fileName = file.getName().substring(0, file.getName().length() - TEMP_POSTFIX.length());
        File completedFile = new File(file.getParent(), fileName);
        boolean renamed = file.renameTo(completedFile);
        if (!renamed) {
            throw new ProxyCacheException("Error renaming file " + file + " to " + completedFile + " for completion!");
        }

        file = completedFile;
        try {
            dataFile = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            throw new ProxyCacheException("Error opening " + file + " as disc cache", e);
        }
    }

    @Override
    public synchronized boolean isCompleted() {
        return !isTempFile(file);
    }

    /**
     * Returns file to be used fo caching. It may as original file passed in constructor as some temp file for not completed cache.
     *
     * @return file for caching.
     */
    public File getFile() {
        return file;
    }

    private boolean isTempFile(File file) {
        return file.getName().endsWith(TEMP_POSTFIX);
    }
}
