package io.github.xiaox.sp4android.cache;


import io.github.xiaox.sp4android.exception.ProxyCacheException;

/**
 * @author X
 * @version V0.1.0
 */
public interface Cache {

    int read(byte[] buffer, int offset, int length) throws ProxyCacheException;

    void append(byte[] data, int length) throws ProxyCacheException;

    void close() throws ProxyCacheException;

    int available() throws ProxyCacheException;

    void complete() throws ProxyCacheException;

    boolean isCompleted();
}
