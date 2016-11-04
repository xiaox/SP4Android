package io.github.xiaox.sp4android.server;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.xiaox.sp4android.cache.file.FileCache;
import io.github.xiaox.sp4android.exception.ProxyCacheException;
import io.github.xiaox.sp4android.model.GetRequest;
import io.github.xiaox.sp4android.proxy.HttpProxyCache;
import io.github.xiaox.sp4android.source.HttpUrlSource;
import io.github.xiaox.sp4android.util.Preconditions;

/**
 * @author X
 * @version 0.1.0
 */

public class HttpProxyCacheServerClients {

    private final String url;
    private final Config config;

    private volatile HttpProxyCache proxyCache;
    private final AtomicInteger clientsCount = new AtomicInteger(0);

    public HttpProxyCacheServerClients(String url, Config config) {
        this.url = Preconditions.checkNotNull(url);
        this.config = Preconditions.checkNotNull(config);
    }

    public void processRequest(GetRequest request, Socket socket) throws ProxyCacheException, IOException {
        startProcessRequest();
        try {
            clientsCount.incrementAndGet();
            proxyCache.processRequest(request, socket);
        } finally {
            finishProcessRequest();
        }
    }

    public void shutDown() {
        if (proxyCache != null) {
            proxyCache.shutdown();
            proxyCache = null;
        }
        clientsCount.set(0);
    }

    private synchronized void startProcessRequest() throws ProxyCacheException {
        proxyCache = proxyCache == null ? newHttpProxyCache() : proxyCache;
    }

    private synchronized void finishProcessRequest() {
        if (clientsCount.decrementAndGet() <= 0) {
            proxyCache.shutdown();
            proxyCache = null;
        }
    }

    private HttpProxyCache newHttpProxyCache() throws ProxyCacheException {
        HttpUrlSource source = new HttpUrlSource(url, config.sourceInfoStorage);
        FileCache cache = new FileCache(config.generateCacheFile(url), config.diskUsage);
        HttpProxyCache httpProxyCache = new HttpProxyCache(source, cache);
        return httpProxyCache;
    }

}
