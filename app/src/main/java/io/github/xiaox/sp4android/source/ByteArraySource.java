package io.github.xiaox.sp4android.source;

import java.io.ByteArrayInputStream;

import io.github.xiaox.sp4android.exception.ProxyCacheException;

/**
 * @author X
 * @version V0.1.0
 */
public class ByteArraySource implements Source {

    private final byte[] data;
    private ByteArrayInputStream arrayInputStream;

    public ByteArraySource(byte[] data) {
        this.data = data;
    }

    @Override
    public void open(int offset) throws ProxyCacheException {
        arrayInputStream = new ByteArrayInputStream(data);
        arrayInputStream.skip(offset);
    }

    @Override
    public int length() throws ProxyCacheException {
        return data.length;
    }

    @Override
    public int read(byte[] buffer) throws ProxyCacheException {
        return arrayInputStream.read(buffer, 0, buffer.length);
    }

    @Override
    public void close() throws ProxyCacheException {
    }
}
