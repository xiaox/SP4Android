package io.github.xiaox.sp4android.source;

import io.github.xiaox.sp4android.exception.ProxyCacheException;

/**
 * @author X
 * @version V0.1.0
 */
public interface Source {

    void open(int offset) throws ProxyCacheException;

    int length() throws ProxyCacheException;

    int read(byte[] buffer) throws ProxyCacheException;

    void close() throws ProxyCacheException;

}
