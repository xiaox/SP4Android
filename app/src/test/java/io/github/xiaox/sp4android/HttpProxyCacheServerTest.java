package io.github.xiaox.sp4android;

import android.util.Pair;

import com.danikula.android.garden.io.Files;
import com.danikula.android.garden.io.IoUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

import io.github.xiaox.sp4android.cache.file.FileNameGenerator;
import io.github.xiaox.sp4android.cache.file.MD5FileNameGenerator;
import io.github.xiaox.sp4android.server.HttpProxyCacheServer;
import io.github.xiaox.sp4android.support.ProxyCacheTestUtils;
import io.github.xiaox.sp4android.support.Response;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author X
 * @version 0.1.0
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class HttpProxyCacheServerTest {

    private File cacheFolder;

    @Before
    public void setUp() throws IOException {
        cacheFolder = ProxyCacheTestUtils.newCacheFile();
        Files.createDirectory(cacheFolder);
        Files.cleanDirectory(cacheFolder);
    }

    @Test
    public void testHttpProxyCache() throws IOException {
        Pair<File, Response> response = readProxyData(ProxyCacheTestUtils.HTTP_DATA_URL);

        assertThat(response.second.code).isEqualTo(200);
        assertThat(response.second.data).isEqualTo(ProxyCacheTestUtils.getFileContent(response.first));
        assertThat(response.second.data).isEqualTo(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_NAME));
    }

    @Test
    public void testProxyContentWIthPartialCache() throws IOException {
        File cacheDir = RuntimeEnvironment.application.getExternalCacheDir();
        File file = new File(cacheDir, new MD5FileNameGenerator().generate(ProxyCacheTestUtils.HTTP_DATA_URL));
        int partialCacheSize = 1000;
        byte[] partialData = ProxyCacheTestUtils.generate(partialCacheSize);
        File partialCacheFile = ProxyCacheTestUtils.getTempFile(file);
        IoUtils.saveToFile(partialData, partialCacheFile);

        HttpProxyCacheServer proxyCacheServer = newProxy(cacheDir);
        Response response = ProxyCacheTestUtils.readProxyResponse(proxyCacheServer, ProxyCacheTestUtils.HTTP_DATA_URL);
        proxyCacheServer.shutdown();

        byte[] expected = ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_NAME);
        System.arraycopy(partialData, 0, expected, 0, partialCacheSize);
        assertThat(response.data).isEqualTo(expected);
    }

    @Test
    public void testProxyFullResponse() throws IOException {
        Pair<File, Response> response = readProxyData(ProxyCacheTestUtils.HTTP_DATA_BIG_URL);

        assertThat(response.second.code).isEqualTo(200);
        assertThat(response.second.contentLength).isEqualTo(ProxyCacheTestUtils.HTTP_DATA_BIG_SIZE);
        assertThat(response.second.contentType).isEqualTo("image/jpeg");
        assertThat(response.second.headers.containsKey("Accept-Ranges")).isTrue();
        assertThat(response.second.headers.get("Accept-Ranges").get(0)).isEqualTo("bytes");
        assertThat(response.second.headers.containsKey("Content-Range")).isFalse();
        assertThat(response.second.data).isEqualTo(ProxyCacheTestUtils.getFileContent(response.first));
        assertThat(response.second.data).isEqualTo(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_BIG_NAME));
    }

    @Test
    public void testMimeFromResponse() throws Exception {
        Pair<File, Response> response = readProxyData("https://raw.githubusercontent.com/danikula/AndroidVideoCache/master/files/android");
        assertThat(response.second.contentType).isEqualTo("application/octet-stream");
    }


    @Test
    public void testProxyFullResponseWithRedirect() throws Exception {
        Pair<File, Response> response = readProxyData(ProxyCacheTestUtils.HTTP_DATA_BIG_URL_ONE_REDIRECT);

        assertThat(response.second.code).isEqualTo(200);
        assertThat(response.second.contentLength).isEqualTo(ProxyCacheTestUtils.HTTP_DATA_BIG_SIZE);
        assertThat(response.second.contentType).isEqualTo("image/jpeg");
        assertThat(response.second.headers.containsKey("Accept-Ranges")).isTrue();
        assertThat(response.second.headers.get("Accept-Ranges").get(0)).isEqualTo("bytes");
        assertThat(response.second.headers.containsKey("Content-Range")).isFalse();
        assertThat(response.second.data).isEqualTo(ProxyCacheTestUtils.getFileContent(response.first));
        assertThat(response.second.data).isEqualTo(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_BIG_NAME));
    }

    private Pair<File, Response> readProxyData(String url) throws IOException {
        return readProxyData(url, -1);
    }

    private Pair<File, Response> readProxyData(String url, int offset) throws IOException {
        File file = file(cacheFolder, url);
        HttpProxyCacheServer proxyCacheServer = newProxy(cacheFolder);

        Response response = ProxyCacheTestUtils.readProxyResponse(proxyCacheServer, url, offset);
        proxyCacheServer.shutdown();
        return new Pair<>(file, response);
    }

    private File file(File parent, String url) {
        FileNameGenerator fileNameGenerator = new MD5FileNameGenerator();
        String name = fileNameGenerator.generate(url);
        return new File(parent, name);
    }

    private HttpProxyCacheServer newProxy(File cacheDir) {
        return new HttpProxyCacheServer.Builder(RuntimeEnvironment.application)
                .cacheDirectory(cacheDir)
                .build();
    }

}
