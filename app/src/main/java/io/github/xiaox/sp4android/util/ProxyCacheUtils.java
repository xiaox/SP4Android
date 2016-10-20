package io.github.xiaox.sp4android.util;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author X
 * @version V0.1.0
 */
public class ProxyCacheUtils {

    static final String LOG_TAG = "ProxyCache";

    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    public static final int MAX_ARRAY_PREVIEW = 16;

    public static String getSupposableMine(String url) {
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        return TextUtils.isEmpty(extension) ? null : mimeTypeMap.getMimeTypeFromExtension(extension);
    }

    public static void assertBuffer(byte[] buffer, int offset, int length) {
        Preconditions.checkNotNull(buffer, "Buffer must be not null");
        Preconditions.checkArgument(offset >= 0, "Data offset must be positive");
        Preconditions.checkArgument(length >= 0 && length <= buffer.length, "Length must be in rang [0..buffer.length]");
    }

    public static String encode(String url) {
        try {
            return URLEncoder.encode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error encoding url", e);
        }
    }

    public static String decode(String url) {
        try {
            return URLDecoder.decode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error decoding url", e);
        }
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error closing resource", e);
            }
        }
    }

    public static String computeMD5(String string) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digestBytes = messageDigest.digest(string.getBytes());
            return bytesToHexString(digestBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
