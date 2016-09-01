package io.github.xiaox.sp4android.sourcestorage;

import io.github.xiaox.sp4android.source.SourceInfo;

/**
 * @author X
 * @version V0.1.0
 */
public interface SourceInfoStorage {

    SourceInfo get(String url);

    void put(String url, SourceInfo sourceInfo);

    void release();
}
