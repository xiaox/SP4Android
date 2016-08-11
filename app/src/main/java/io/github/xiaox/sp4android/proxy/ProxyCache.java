package io.github.xiaox.sp4android.proxy;

import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

import io.github.xiaox.sp4android.cache.Cache;
import io.github.xiaox.sp4android.exception.InterruptedProxyCacheException;
import io.github.xiaox.sp4android.exception.ProxyCacheException;
import io.github.xiaox.sp4android.source.Source;
import io.github.xiaox.sp4android.util.ProxyCacheUtils;


/**
 * @author X
 * @version V0.1.0
 */
public class ProxyCache {

    private static final String LOG_TAG = "ProxyCache";
    private static final int MAX_READ_SOURCE_ATTEMPTS = 1;

    private final Source source;
    private final Cache cache;

    private final AtomicInteger readSourceErrorsCount;
    private volatile Thread sourceReaderThread;

    private final Object wc = new Object();
    private final Object stopLock = new Object();

    private volatile boolean stopped;
    private volatile int percentAvailable = -1;

    public ProxyCache(Source source, Cache cache) {
        this.source = source;
        this.cache = cache;
        readSourceErrorsCount = new AtomicInteger();
    }

    public int read(byte[] buffer, int offset, int length) throws ProxyCacheException {
        ProxyCacheUtils.assertBuffer(buffer, offset, length);

        while (!cache.isCompleted() && cache.available() < (offset + length) && !stopped) {
            readSourceAsync();
            waitForSourceData();
            checkReadSourceErrorCount();
        }

        int read = cache.read(buffer, offset, length);

        if (cache.isCompleted() && percentAvailable != 100) {
            percentAvailable = 100;
            onCachePercentsAvailableChanged(100);
        }
        return read;
    }

    private void readSourceAsync() throws ProxyCacheException {
        boolean readingInProcess = sourceReaderThread != null && sourceReaderThread.getState() != Thread.State.TERMINATED;
        if (!stopped && !cache.isCompleted() && !readingInProcess) {
            sourceReaderThread = new Thread(new SourceReadRunnable(), "Source reader for " + source);
            sourceReaderThread.start();
        }
    }

    private void waitForSourceData() throws ProxyCacheException {
        synchronized (wc) {
            try {
                wc.wait(1000);
            } catch (InterruptedException e) {
                throw new ProxyCacheException("Waiting source data is interrupted!", e);
            }
        }
    }

    private void checkReadSourceErrorCount() throws ProxyCacheException {
        int errorCount = readSourceErrorsCount.get();
        if (errorCount >= MAX_READ_SOURCE_ATTEMPTS) {
            readSourceErrorsCount.set(0);
            throw new ProxyCacheException("Error reading source " + errorCount + " times");
        }
    }

    protected void onCachePercentsAvailableChanged(int percentsAvailable) {
    }

    public void shutdown() {
        synchronized (stopLock) {

            Log.e(LOG_TAG, "Shutdown proxy for " + source);

            stopped = true;
            if (sourceReaderThread != null) {
                sourceReaderThread.interrupt();
            }
            try {
                cache.close();
            } catch (ProxyCacheException e) {
                onError(e);
            }
        }
    }

    private void readSource() {
        int sourceAvailable = -1;
        int offset = 0;

        try {
            offset = cache.available();
            source.open(offset);
            sourceAvailable = source.length();
            byte[] buffer = new byte[ProxyCacheUtils.DEFAULT_BUFFER_SIZE];
            int readBytes;
            while ((readBytes = source.read(buffer)) != -1) {
                synchronized (stopLock) {
                    if (isStopped()) {
                        return;
                    }
                    cache.append(buffer, readBytes);
                }
                offset += readBytes;
                notifyNewCacheDataAvailable(offset, sourceAvailable);
            }
            tryComplete();
        } catch (Throwable e) {
            readSourceErrorsCount.incrementAndGet();
            onError(e);
        } finally {
            closeSource();
            notifyNewCacheDataAvailable(offset, sourceAvailable);
        }

    }


    private boolean isStopped() {
        return Thread.currentThread().isInterrupted() || stopped;
    }

    private void notifyNewCacheDataAvailable(int cacheAvailable, int sourceAvailable) {

        onCacheAvailable(cacheAvailable, sourceAvailable);
        synchronized (wc) {
            wc.notifyAll();
        }
    }

    private void onCacheAvailable(int cacheAvailable, int sourceLength) {
        boolean zeroLengthSource = sourceLength == 0;

        int percents = zeroLengthSource ? 100 : cacheAvailable * 100 / sourceLength;

        boolean percentsChanged = percents != percentAvailable;
        boolean sourceLengthKnow = sourceLength >= 0;
        if (sourceLengthKnow && percentsChanged) {
            onCachePercentsAvailableChanged(percents);
        }
        percentAvailable = percents;

    }

    private void tryComplete() throws ProxyCacheException {
        synchronized (stopLock) {
            if (!isStopped() && cache.available() == source.length()) {
                cache.complete();
            }
        }
    }

    private void closeSource() {
        try {
            source.close();
        } catch (ProxyCacheException e) {
            onError(new ProxyCacheException("Error closing source " + source, e));
        }
    }

    protected final void onError(final Throwable e) {
        boolean interruption = e instanceof InterruptedProxyCacheException;
        if (interruption) {
            Log.d(LOG_TAG, "ProxyCache is interrupted");
        } else {
            Log.d(LOG_TAG, "ProxyCache error", e);
        }

    }

    private class SourceReadRunnable implements Runnable {
        @Override
        public void run() {
            readSource();
        }
    }


}
