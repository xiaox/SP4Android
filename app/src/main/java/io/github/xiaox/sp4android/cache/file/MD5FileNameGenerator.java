package io.github.xiaox.sp4android.cache.file;

import android.text.TextUtils;

import io.github.xiaox.sp4android.util.ProxyCacheUtils;

/**
 * @author X
 * @version 0.1.0
 */

public class MD5FileNameGenerator implements FileNameGenerator {

    private final int MAX_EXTENSION_LENGTH = 4;

    @Override
    public String generate(String url) {
        String extension = getExension(url);
        String name = ProxyCacheUtils.computeMD5(url);
        return TextUtils.isEmpty(extension) ? name : name + "." + extension;
    }

    private String getExension(String url) {
        int dotIndex = url.lastIndexOf('.');
        int slashIndex = url.lastIndexOf('/');
        return dotIndex != -1 && dotIndex > slashIndex && dotIndex + 2 + MAX_EXTENSION_LENGTH > url.length() ? url.substring(dotIndex + 1, url.length()) : "";
    }
}
