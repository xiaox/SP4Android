package io.github.xiaox.sp4android;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import io.github.xiaox.sp4android.exception.ProxyCacheException;
import io.github.xiaox.sp4android.source.HttpUrlSource;
import io.github.xiaox.sp4android.source.Source;
import io.github.xiaox.sp4android.sourcestorage.SourceInfoStorage;
import io.github.xiaox.sp4android.sourcestorage.SourceInfoStorageFactory;
import io.github.xiaox.sp4android.support.ProxyCacheTestUtils;
import io.github.xiaox.sp4android.util.ProxyCacheUtils;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;

/**
 * @author X
 * @version V0.1.0
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class HttpUrlSourceTest {

    @Test
    public void testHttpUrlSourceRange() throws ProxyCacheException, IOException {
        int offset = 1000;
        int length = 10;
        Source source = new HttpUrlSource(ProxyCacheTestUtils.HTTP_DATA_URL);
        source.open(offset);

        byte[] readData = new byte[length];
        source.read(readData);
        source.close();

        byte[] expectedData = Arrays.copyOfRange(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_NAME), offset, offset + length);
        assertThat(readData).isEqualTo(expectedData);

    }

    @Test
    public void testHttpUrlSourceWithOffset() throws ProxyCacheException, IOException {
        int offset = 3000;
        Source source = new HttpUrlSource(ProxyCacheTestUtils.HTTP_DATA_BIG_URL);
        source.open(offset);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int read;
        byte[] buffer = new byte[3000];
        while ((read = source.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        source.close();

        byte[] expectedData = Arrays.copyOfRange(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_BIG_NAME), offset, ProxyCacheTestUtils.HTTP_DATA_BIG_SIZE);
        assertThat(outputStream.toByteArray()).isEqualTo(expectedData);
    }


    @Test
    public void testFetchContentLength() throws Exception {
        Source source = new HttpUrlSource(ProxyCacheTestUtils.HTTP_DATA_URL);
        assertThat(source.length()).isEqualTo(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_NAME).length);
    }

    @Test
    public void testFetchInfoWithRedirect() throws Exception {
        HttpUrlSource source = new HttpUrlSource(ProxyCacheTestUtils.HTTP_DATA_URL_ONE_REDIRECT);
        source.open(0);
        int available = source.length();
        String mime = source.getMime();
        source.close();

        assertThat(available).isEqualTo(ProxyCacheTestUtils.HTTP_DATA_SIZE);
        assertThat(mime).isEqualTo("image/jpeg");
    }

    @Test
    public void testFetchDataWithRedirect() throws Exception {
        HttpUrlSource source = new HttpUrlSource(ProxyCacheTestUtils.HTTP_DATA_URL_ONE_REDIRECT);
        source.open(0);
        byte[] readData = new byte[ProxyCacheTestUtils.HTTP_DATA_SIZE];
        readSource(source, readData);
        source.close();

        byte[] expectedData = Arrays.copyOfRange(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_NAME), 0, ProxyCacheTestUtils.HTTP_DATA_SIZE);
        assertThat(readData).isEqualTo(expectedData);
    }

    @Test
    public void testFetchPartialDataWithRedirect() throws Exception {
        int offset = 42;
        HttpUrlSource source = new HttpUrlSource(ProxyCacheTestUtils.HTTP_DATA_URL_ONE_REDIRECT);
        source.open(offset);
        byte[] readData = new byte[ProxyCacheTestUtils.HTTP_DATA_SIZE - offset];
        readSource(source, readData);
        source.close();

        byte[] expectedData = Arrays.copyOfRange(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_NAME), offset, ProxyCacheTestUtils.HTTP_DATA_SIZE);
        assertThat(readData).isEqualTo(expectedData);
    }

    @Test
    public void testFetchPartialDataWithMultiRedirects() throws Exception {
        int offset = 42;
        HttpUrlSource source = new HttpUrlSource(ProxyCacheTestUtils.HTTP_DATA_URL_3_REDIRECTS);
        source.open(offset);
        byte[] readData = new byte[ProxyCacheTestUtils.HTTP_DATA_SIZE - offset];
        readSource(source, readData);
        source.close();

        byte[] expectedData = Arrays.copyOfRange(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_NAME), offset, ProxyCacheTestUtils.HTTP_DATA_SIZE);
        assertThat(readData).isEqualTo(expectedData);
    }

    @Ignore("To test it fairly we should disable caching connection.setUseCaches(false), but it will decrease performance")
    @Test(expected = ProxyCacheException.class)
    public void testExceedingRedirects() throws Exception {
        HttpUrlSource source = new HttpUrlSource(ProxyCacheTestUtils.HTTP_DATA_URL_6_REDIRECTS);
        source.open(0);
        fail("Too many redirects");
    }

    @Ignore("Seems Robolectric bug: MimeTypeMap.getFileExtensionFromUrl always returns null")
    @Test
    public void testMimeByUrl() throws Exception {
        assertThat(new HttpUrlSource("http://mysite.by/video.mp4").getMime()).isEqualTo("video/mp4");
        assertThat(new HttpUrlSource(ProxyCacheTestUtils.HTTP_DATA_URL).getMime()).isEqualTo("image/jpeg");
    }

    @Test(expected = RuntimeException.class)
    public void testAngryHttpUrlSourceLength() throws Exception {
        ProxyCacheTestUtils.newAngryHttpUrlSource().length();
        fail("source.length() should throw exception");
    }

    @Test(expected = RuntimeException.class)
    public void testAngryHttpUrlSourceOpen() throws Exception {
        ProxyCacheTestUtils.newAngryHttpUrlSource().open(Mockito.anyInt());
        fail("source.open() should throw exception");
    }

    @Test(expected = RuntimeException.class)
    public void testAngryHttpUrlSourceRead() throws Exception {
        ProxyCacheTestUtils.newAngryHttpUrlSource().read(any(byte[].class));
        fail("source.read() should throw exception");
    }

    @Test(expected = RuntimeException.class)
    public void testNotOpenableHttpUrlSourceOpen() throws Exception {
        SourceInfoStorage sourceInfoStorage = SourceInfoStorageFactory.newEmptySourceInfoStorage();
        ProxyCacheTestUtils.newNotOpenableHttpUrlSource("", sourceInfoStorage).open(Mockito.anyInt());
        fail("source.open() should throw exception");
    }

    private void readSource(Source source, byte[] target) throws ProxyCacheException {
        byte[] buffer = new byte[ProxyCacheUtils.DEFAULT_BUFFER_SIZE];
        int totalRead = 0;
        int readBytes;
        while ((readBytes = source.read(buffer)) != -1) {
            System.arraycopy(buffer, 0, target, totalRead, readBytes);
            totalRead += readBytes;
        }
    }
}
