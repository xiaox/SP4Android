package io.github.xiaox.sp4android.cache;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import io.github.xiaox.sp4android.exception.ProxyCacheException;
import io.github.xiaox.sp4android.util.Preconditions;


/**
 * @author X
 * @version V0.1.0
 */
public class ByteArrayCache implements Cache {

    private volatile byte[] data;
    private volatile boolean completed;

    public ByteArrayCache() {
        this(new byte[0]);
    }

    public ByteArrayCache(byte[] data) {
        this.data = Preconditions.checkNotNull(data);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws ProxyCacheException {

        if (offset >= data.length) {
            return -1;
        }

        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too long offset for memory cache " + offset);
        }
        return new ByteArrayInputStream(data).read(buffer, offset, length);

    }

    @Override
    public void append(byte[] newData, int length) throws ProxyCacheException {
        Preconditions.checkNotNull(newData);
        Preconditions.checkArgument(length >= 0 && length <= newData.length);

        byte[] appendedData = Arrays.copyOf(data, data.length + length);
        System.arraycopy(newData, 0, appendedData, data.length, length);
        data = appendedData;
    }

    @Override
    public void close() throws ProxyCacheException {

    }

    @Override
    public int available() throws ProxyCacheException {
        return data.length;
    }

    @Override
    public void complete() throws ProxyCacheException {
        completed = true;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }
}
