package io.github.xiaox.sp4android.source;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import io.github.xiaox.sp4android.exception.ProxyCacheException;
import io.github.xiaox.sp4android.sourcestorage.SourceInfoStorage;
import io.github.xiaox.sp4android.sourcestorage.SourceInfoStorageFactory;
import io.github.xiaox.sp4android.util.ProxyCacheUtils;

/**
 * @author X
 * @version V0.1.0
 */
public class HttpUrlSource implements Source {

    private static final String LOG_TAG = "HttpUrlSource";

    private static final int MAX_REDIRECTS = 5;
    private final SourceInfoStorage sourceInfoStorage;
    private SourceInfo sourceInfo;
    private HttpURLConnection connection;
    private InputStream inputStream;

    public HttpUrlSource(String url) {
        this(url, SourceInfoStorageFactory.newEmptySourceInfoStorage());
    }

    public HttpUrlSource(HttpUrlSource source) {
        this.sourceInfo = source.sourceInfo;
        this.sourceInfoStorage = source.sourceInfoStorage;
    }

    public HttpUrlSource(String url, SourceInfoStorage sourceInfoStorage) {
        this.sourceInfoStorage = sourceInfoStorage;
        SourceInfo sourceInfo = sourceInfoStorage.get(url);
        this.sourceInfo = sourceInfo != null ? sourceInfo : new SourceInfo(url, Integer.MIN_VALUE, ProxyCacheUtils.getSupposableMine(url));
    }

    @Override
    public void open(int offset) throws ProxyCacheException {
        try {
            connection = openConnection(offset, -1);

            String mime = connection.getContentType();
            inputStream = new BufferedInputStream(connection.getInputStream(), ProxyCacheUtils.DEFAULT_BUFFER_SIZE);
            int len = readSourceAvailableBytes(connection, offset, connection.getResponseCode());

            this.sourceInfo = new SourceInfo(sourceInfo.url, len, mime);
            this.sourceInfoStorage.put(sourceInfo.url, sourceInfo);
        } catch (IOException e) {
            throw new ProxyCacheException("Error opening connection for " + sourceInfo.url + " with offset " + offset, e);
        }
    }

    @Override
    public synchronized int length() throws ProxyCacheException {

        if (sourceInfo.length == Integer.MIN_VALUE) {
            fetchContentInfo();
        }
        return sourceInfo.length;
    }

    @Override
    public int read(byte[] buffer) throws ProxyCacheException {

        if (inputStream == null) {
            throw new ProxyCacheException("Error reading data from " + sourceInfo.url + ": connection is absent!");
        }
        try {
            return inputStream.read(buffer, 0, buffer.length);
        } catch (IOException e) {
            throw new ProxyCacheException("Error reading data from " + sourceInfo.url, e);
        }
    }

    @Override
    public void close() throws ProxyCacheException {
        if (connection != null) {
            connection.disconnect();
        }
    }

    private HttpURLConnection openConnection(int offset, int timeout) throws IOException, ProxyCacheException {

        HttpURLConnection connection;
        boolean redirected;
        int redirectCount = 0;
        String url = this.sourceInfo.url;
        do {
            connection = (HttpURLConnection) new URL(url).openConnection();

            if (offset > 0) {
                connection.setRequestProperty("Range", "bytes=" + offset + "-");
            }

            if (timeout > 0) {
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);
            }

            int code = connection.getResponseCode();
            redirected = code == HttpURLConnection.HTTP_MOVED_PERM
                    || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == HttpURLConnection.HTTP_SEE_OTHER;
            if (redirected) {
                url = connection.getHeaderField("Location");
                redirectCount++;
                connection.disconnect();
            }
            if (redirectCount > MAX_REDIRECTS) {
                throw new ProxyCacheException("Too many redirects: " + redirectCount);
            }

        } while (redirected);

        return connection;

    }

    private int readSourceAvailableBytes(HttpURLConnection connection, int offset, int responseCode) {
        int contentLength = connection.getContentLength();
        return responseCode == HttpURLConnection.HTTP_OK ? contentLength
                : responseCode == HttpURLConnection.HTTP_PARTIAL ? contentLength + offset : sourceInfo.length;
    }

    private void fetchContentInfo() {
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;

        try {
            urlConnection = openConnection(0, 10000);

            int len = urlConnection.getContentLength();
            String mime = urlConnection.getContentType();
            inputStream = urlConnection.getInputStream();

            this.sourceInfo = new SourceInfo(sourceInfo.url, len, mime);
            this.sourceInfoStorage.put(sourceInfo.url, sourceInfo);

        } catch (IOException | ProxyCacheException e) {
            //DO NOTHING
            Log.e(LOG_TAG, "Error fetching info from " + sourceInfo.url, e);
        } finally {
            ProxyCacheUtils.close(inputStream);
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

    }


    public synchronized String getMime() throws ProxyCacheException {
        if (TextUtils.isEmpty(sourceInfo.mime)) {
            fetchContentInfo();
        }
        return sourceInfo.mime;
    }

    public String getUrl() {
        return sourceInfo.url;
    }

    @Override
    public String toString() {
        return "HttpUrlSource{sourceInfo='" + sourceInfo + "}";
    }
}
