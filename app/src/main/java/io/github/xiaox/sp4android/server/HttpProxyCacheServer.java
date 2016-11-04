package io.github.xiaox.sp4android.server;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.xiaox.sp4android.cache.file.DiskUsage;
import io.github.xiaox.sp4android.cache.file.FileNameGenerator;
import io.github.xiaox.sp4android.cache.file.MD5FileNameGenerator;
import io.github.xiaox.sp4android.cache.file.TotalSizeLruDiskUsage;
import io.github.xiaox.sp4android.exception.ProxyCacheException;
import io.github.xiaox.sp4android.model.GetRequest;
import io.github.xiaox.sp4android.sourcestorage.SourceInfoStorage;
import io.github.xiaox.sp4android.sourcestorage.SourceInfoStorageFactory;
import io.github.xiaox.sp4android.util.Preconditions;
import io.github.xiaox.sp4android.util.ProxyCacheUtils;
import io.github.xiaox.sp4android.util.StorageUtils;

/**
 * @author X
 * @version 0.1.0
 */

public class HttpProxyCacheServer {

    private final String LOG_TAG = "cacheServer";

    private final String PROXY_HOST = "127.0.0.1";

    private final Object clientsLock = new Object();
    private final ExecutorService socketProcessor = Executors.newFixedThreadPool(8);
    private final Map<String, HttpProxyCacheServerClients> clientsMap = new ConcurrentHashMap<>();
    private final ServerSocket serverSocket;
    private final int port;
    private final Config config;
    private final Pinger pinger;

    private final Thread waitConnectionThread;

    public HttpProxyCacheServer(Context context) {
        this(new Builder(context).buildConfig());
    }

    private HttpProxyCacheServer(Config config) {
        this.config = Preconditions.checkNotNull(config);
        try {
            InetAddress inetAddress = InetAddress.getByName(PROXY_HOST);
            serverSocket = new ServerSocket(0, 8, inetAddress);
            port = serverSocket.getLocalPort();
            CountDownLatch startSignal = new CountDownLatch(1);
            waitConnectionThread = new Thread(new WaitRequestRunnable(startSignal));
            waitConnectionThread.start();
            startSignal.await();
            pinger = new Pinger(PROXY_HOST, port);
            Log.d(LOG_TAG, "Proxy cache Server started. Is it alive? " + isAlive());

        } catch (IOException | InterruptedException e) {
            socketProcessor.shutdown();
            throw new IllegalStateException(e);
        }

    }

    private final class SocketProcessorRunnable implements Runnable {
        private final Socket socket;

        public SocketProcessorRunnable(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            processSocket(socket);
        }
    }

    private void processSocket(Socket socket) {
        try {
            GetRequest request = GetRequest.read(socket.getInputStream());
            String url = ProxyCacheUtils.decode(request.uri);

            if (pinger.isPingRequest(url)) {
                pinger.responseToPing(socket);
            } else {
                HttpProxyCacheServerClients clients = getClients(url);
                clients.processRequest(request, socket);
            }

        } catch (ProxyCacheException | IOException e) {
            e.printStackTrace();
        } finally {
            releaseSocket(socket);
        }
    }

    private HttpProxyCacheServerClients getClients(String url) {
        synchronized (clientsLock) {
            HttpProxyCacheServerClients clients = clientsMap.get(url);
            if (clients == null) {
                clients = new HttpProxyCacheServerClients(url, config);
                clientsMap.put(url, clients);
            }
            return clients;
        }
    }

    public String getProxyUrl(String url, boolean allowCachedFileUri) {
        if (allowCachedFileUri && isCached(url)) {
            File cacheFile = getCacheFile(url);
            touchFileSafely(cacheFile);
            return Uri.fromFile(cacheFile).toString();
        }
        return isAlive() ? appendToProxyUrl(url) : url;
    }

    public void shutdown() {
        shutdownClients();
        config.sourceInfoStorage.release();

        waitConnectionThread.interrupt();

        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void shutdownClients() {
        synchronized (clientsLock) {
            for (HttpProxyCacheServerClients client : clientsMap.values()) {
                client.shutDown();
            }
            clientsMap.clear();
        }
    }

    public boolean isCached(String url) {
        Preconditions.checkNotNull(url);
        return getCacheFile(url).exists();
    }

    private File getCacheFile(String url) {
        File cacheDir = config.cacheRoot;
        String fileName = config.fileNameGenerator.generate(url);
        return new File(cacheDir, fileName);
    }

    private void touchFileSafely(File cacheFile) {
        try {
            config.diskUsage.touch(cacheFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isAlive() {
        return pinger.ping(3, 70);
    }

    private String appendToProxyUrl(String url) {
        return String.format(Locale.US, "http://%s:%d/%s", PROXY_HOST, port, ProxyCacheUtils.encode(url));
    }

    private void releaseSocket(Socket socket) {
        closeSocketInput(socket);
        closeSocketOutput(socket);
        closeSocket(socket);
    }

    private void closeSocketInput(Socket socket) {
        if (!socket.isInputShutdown()) {
            try {
                socket.shutdownInput();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeSocketOutput(Socket socket) {
        if (!socket.isOutputShutdown()) {
            try {
                socket.shutdownOutput();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeSocket(Socket socket) {
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private final class WaitRequestRunnable implements Runnable {

        private final CountDownLatch startSignal;

        WaitRequestRunnable(CountDownLatch startSignal) {
            this.startSignal = startSignal;
        }

        @Override
        public void run() {
            startSignal.countDown();
            waitForRequest();
        }
    }

    private void waitForRequest() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                Log.d(LOG_TAG, "New socket accepted " + socket);
                socketProcessor.submit(new SocketProcessorRunnable(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static final class Builder {

        private final long DEFAULT_MAX_SIZE = 128 * 1024 * 1024;

        private File cacheRoot;
        private FileNameGenerator fileNameGenerator;
        private DiskUsage diskUsage;
        private SourceInfoStorage sourceInfoStorage;

        public Builder(Context context) {
            cacheRoot = StorageUtils.getIndividualCacheDirectory(context);
            fileNameGenerator = new MD5FileNameGenerator();
            diskUsage = new TotalSizeLruDiskUsage(DEFAULT_MAX_SIZE);
            sourceInfoStorage = SourceInfoStorageFactory.newSourceInfoStorage(context);
        }

        public Builder cacheDirectory(File file) {
            this.cacheRoot = Preconditions.checkNotNull(file);
            return this;
        }

        public Builder fileNameGenerator(FileNameGenerator nameGenerator) {
            this.fileNameGenerator = Preconditions.checkNotNull(nameGenerator);
            return this;
        }

        public Builder maxCacheSize(long maxSize) {
            if (maxSize <= 0) return this;
            this.diskUsage = new TotalSizeLruDiskUsage(maxSize);
            return this;
        }

        public HttpProxyCacheServer build() {
            return new HttpProxyCacheServer(buildConfig());
        }

        private Config buildConfig() {
            return new Config(cacheRoot, fileNameGenerator, diskUsage, sourceInfoStorage);
        }
    }


}
