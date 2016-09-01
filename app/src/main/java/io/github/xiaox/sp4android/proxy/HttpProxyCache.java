package io.github.xiaox.sp4android.proxy;

import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import io.github.xiaox.sp4android.cache.CacheListener;
import io.github.xiaox.sp4android.cache.file.FileCache;
import io.github.xiaox.sp4android.exception.ProxyCacheException;
import io.github.xiaox.sp4android.model.GetRequest;
import io.github.xiaox.sp4android.source.HttpUrlSource;
import io.github.xiaox.sp4android.util.ProxyCacheUtils;

/**
 * @author X
 * @version V0.1.0
 */
public class HttpProxyCache extends ProxyCache {

    private static final float NO_CACHE_BARRIER = .2f;

    private final HttpUrlSource source;
    private final FileCache cache;

    private CacheListener listener;

    public HttpProxyCache(HttpUrlSource source, FileCache cache) {
        super(source, cache);

        this.source = source;
        this.cache = cache;
    }

    public void registerCacheListener(CacheListener listener) {
        this.listener = listener;
    }

    public void processRequest(GetRequest request, Socket socket) throws IOException, ProxyCacheException {
        OutputStream out = new BufferedOutputStream(socket.getOutputStream());
        String responseHeaders = newResponseHeaders(request);
        out.write(responseHeaders.getBytes("UTF-8"));

        long offset = request.rangeOffset;
        if (isUseCache(request)) {
            responseWithCache(out, (int) offset);
        } else {
            responseWithoutCache(out, offset);
        }
    }

    private String newResponseHeaders(GetRequest request) throws ProxyCacheException {
        String mime = source.getMime();
        boolean mimeKnown = !TextUtils.isEmpty(mime);
        int length = cache.isCompleted() ? cache.available() : source.length();
        boolean lengthKnown = length > 0;
        long contentLength = request.partial ? length - request.rangeOffset : length;
        boolean addRange = lengthKnown && request.partial;

        return new StringBuilder()
                .append(request.partial ? "HTTP/1.1 206 PARTIAL CONTENT\n" : "HTTP/1.1 200 OK\n")
                .append("Accept-Ranges: bytes\n")
                .append(lengthKnown ? String.format("Content-Length: %d\n", contentLength) : "")
                .append(addRange ? String.format("Content-Range: bytes %d-%d/%d\n", request.rangeOffset, length - 1, length) : "")
                .append(mimeKnown ? String.format("Content-Type: %s\n", mime) : "")
                .append("\n") // headers end
                .toString();

    }

    private boolean isUseCache(GetRequest request) throws ProxyCacheException {
        int sourceLength = source.length();
        boolean sourceLengthKnown = sourceLength > 0;
        int cacheAvailable = cache.available();
        // do not use cache for partial requests which too far from available cache. It seems user seek video.
        return !sourceLengthKnown || !request.partial || request.rangeOffset <= cacheAvailable + sourceLength * NO_CACHE_BARRIER;

    }

    private void responseWithCache(OutputStream out, int offset) throws ProxyCacheException, IOException {
        byte[] buffer = new byte[ProxyCacheUtils.DEFAULT_BUFFER_SIZE];
        int readBytes;
        while ((readBytes = read(buffer, offset, buffer.length)) != -1) {
            out.write(buffer, 0, readBytes);
            offset += readBytes;
        }
        out.flush();
    }

    private void responseWithoutCache(OutputStream out, long offset) throws ProxyCacheException, IOException {
        HttpUrlSource newSourceNoCache = new HttpUrlSource(this.source);
        try {
            newSourceNoCache.open((int) offset);
            byte[] buffer = new byte[ProxyCacheUtils.DEFAULT_BUFFER_SIZE];
            int readBytes;
            while ((readBytes = newSourceNoCache.read(buffer)) != -1) {
                out.write(buffer, 0, readBytes);
                offset += readBytes;
            }
            out.flush();
        } finally {
            newSourceNoCache.close();
        }
    }

    @Override
    protected void onCachePercentsAvailableChanged(int percents) {
        if (listener != null) {
            listener.onCacheAvailable(cache.file, source.getUrl(), percents);
        }
    }
}
