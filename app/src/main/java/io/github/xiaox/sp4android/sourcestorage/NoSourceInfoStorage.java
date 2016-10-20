package io.github.xiaox.sp4android.sourcestorage;

import io.github.xiaox.sp4android.source.SourceInfo;

/**
 * @author X
 * @version V0.1.0
 */
public class NoSourceInfoStorage implements SourceInfoStorage {
    @Override
    public SourceInfo get(String url) {
        return null;
    }

    @Override
    public void put(String url, SourceInfo sourceInfo) {

    }

    @Override
    public void release() {

    }
}
