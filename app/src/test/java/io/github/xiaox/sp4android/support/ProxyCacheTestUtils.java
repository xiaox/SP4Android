package io.github.xiaox.sp4android.support;

import com.danikula.android.garden.io.IoUtils;
import com.google.common.io.Files;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.UUID;

import io.github.xiaox.sp4android.exception.ProxyCacheException;
import io.github.xiaox.sp4android.server.HttpProxyCacheServer;
import io.github.xiaox.sp4android.source.ByteArraySource;
import io.github.xiaox.sp4android.source.HttpUrlSource;
import io.github.xiaox.sp4android.source.Source;
import io.github.xiaox.sp4android.sourcestorage.SourceInfoStorage;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author X
 * @version V0.1.0
 */
public class ProxyCacheTestUtils {
    public static final String HTTP_DATA_URL = "https://raw.githubusercontent.com/danikula/AndroidVideoCache/master/files/android.jpg";
    public static final String HTTP_DATA_URL_ONE_REDIRECT = "http://dwz.cn/3VHZKm";
    public static final String HTTP_DATA_URL_3_REDIRECTS = "http://dwz.cn/3VI1VP";
    public static final String HTTP_DATA_URL_6_REDIRECTS = "http://dwz.cn/3VI7Hh";
    public static final String HTTP_DATA_BIG_URL = "https://raw.githubusercontent.com/danikula/AndroidVideoCache/master/files/phones.jpg";
    public static final String HTTP_DATA_BIG_URL_ONE_REDIRECT = " http://dwz.cn/3VI856";
    public static final String ASSETS_DATA_NAME = "android.jpg";
    public static final String ASSETS_DATA_BIG_NAME = "phones.jpg";
    public static final int HTTP_DATA_SIZE = 4768;
    public static final int HTTP_DATA_BIG_SIZE = 94363;

    public static Response readProxyResponse(HttpProxyCacheServer proxy, String url) throws IOException {
        return readProxyResponse(proxy, url, -1);
    }

    public static Response readProxyResponse(HttpProxyCacheServer proxy, String url, int offset) throws IOException {
        String proxyUrl = proxy.getProxyUrl(url, false);
        if (!proxyUrl.startsWith("http://127.0.0.1")) {
            throw new IllegalStateException("Proxy url" + proxyUrl + " is not proxied");
        }
        URL proxiedUrl = new URL(proxyUrl);
        HttpURLConnection connection = (HttpURLConnection) proxiedUrl.openConnection();
        try {
            if (offset > 0) {
                connection.setRequestProperty("Range", "bytes=" + offset + "-");
            }
            return new Response(connection);
        } finally {
            connection.disconnect();
        }
    }

    public static byte[] loadTestData() throws IOException {
        return loadAssetFile(ASSETS_DATA_NAME);
    }

    public static byte[] loadAssetFile(String name) throws IOException {
        InputStream in = RuntimeEnvironment.application.getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IoUtils.copy(in, out);
        IoUtils.closeSilently(in);
        IoUtils.closeSilently(out);
        return out.toByteArray();
    }

    public static byte[] getFileContent(File file) throws IOException {
        return Files.asByteSource(file).read();
    }

    public static File getTempFile(File file) {
        return new File(file.getParentFile(), file.getName() + ".download");
    }

    public static File newCacheFile() {
        return new File(RuntimeEnvironment.application.getCacheDir(), UUID.randomUUID().toString());
    }

    public static byte[] generate(int capacity) {
        Random random = new Random(System.currentTimeMillis());
        byte[] result = new byte[capacity];
        random.nextBytes(result);
        return result;
    }

    public static HttpUrlSource newAngryHttpUrlSource() throws ProxyCacheException {
        HttpUrlSource source = mock(HttpUrlSource.class);
        doThrow(new RuntimeException()).when(source).getMime();
        doThrow(new RuntimeException()).when(source).read(any(byte[].class));
        doThrow(new RuntimeException()).when(source).open(anyInt());
        doThrow(new RuntimeException()).when(source).length();
        doThrow(new RuntimeException()).when(source).getUrl();
        doThrow(new RuntimeException()).when(source).close();
        return source;
    }

    public static HttpUrlSource newNotOpenableHttpUrlSource(String url, SourceInfoStorage sourceInfoStorage) throws ProxyCacheException {
        HttpUrlSource httpUrlSource = new HttpUrlSource(url, sourceInfoStorage);
        HttpUrlSource source = spy(httpUrlSource);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.out.print("Can't open!!!");
                throw new RuntimeException();
            }
        }).when(source).open(anyInt());
        return source;
    }

    public static Source newPhlegmaticSource(byte[] data, final int maxDelayMs) throws ProxyCacheException {
        Source spySource = spy(new ByteArraySource(data));
        final Random delayGenerator = new Random(System.currentTimeMillis());
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(delayGenerator.nextInt(maxDelayMs));
                return null;
            }
        }).doCallRealMethod().when(spySource).read(any(byte[].class));
        return spySource;
    }
}
