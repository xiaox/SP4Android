package io.github.xiaox.sp4android.sourcestorage;

import android.content.Context;

/**
 * @author X
 * @version V0.1.0
 */
public class SourceInfoStorageFactory {

    public static SourceInfoStorage newSourceInfoStorage(Context context) {
        return new DatabaseSourceInfoStorage(context);
    }

    public static SourceInfoStorage newEmptySourceInfoStorage() {
        return new NoSourceInfoStorage();
    }
}
