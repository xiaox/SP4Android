package io.github.xiaox.sp4android;

import com.danikula.android.garden.io.Files;
import com.danikula.android.garden.io.IoUtils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import io.github.xiaox.sp4android.cache.Cache;
import io.github.xiaox.sp4android.cache.file.FileCache;
import io.github.xiaox.sp4android.exception.ProxyCacheException;
import io.github.xiaox.sp4android.support.ProxyCacheTestUtils;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author X
 * @version V0.1.0
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class FileCacheTest {

    @Test
    public void testWriteReadDiscCache() throws ProxyCacheException, IOException {
        int firstPortionLength = 10000;
        byte[] firstDataPortion = ProxyCacheTestUtils.generate(firstPortionLength);
        File file = ProxyCacheTestUtils.newCacheFile();
        Cache fileCache = new FileCache(file);

        fileCache.append(firstDataPortion, firstDataPortion.length);
        byte[] readData = new byte[firstPortionLength];
        fileCache.read(readData, 0, firstPortionLength);
        assertThat(readData).isEqualTo(firstDataPortion);
        byte[] fileContent = ProxyCacheTestUtils.getFileContent(ProxyCacheTestUtils.getTempFile(file));
        assertThat(readData).isEqualTo(fileContent);

    }

    @Test
    public void testFileCacheCompletion() throws Exception {
        File file = ProxyCacheTestUtils.newCacheFile();
        File tmpFile = ProxyCacheTestUtils.getTempFile(file);

        Cache fileCache = new FileCache(file);
        assertThat(file.exists()).isFalse();
        assertThat(tmpFile.exists()).isTrue();

        int dataSize = 345;
        fileCache.append(ProxyCacheTestUtils.generate(dataSize), dataSize);
        fileCache.complete();

        assertThat(file.exists()).isTrue();
        assertThat(tmpFile.exists()).isFalse();
        assertThat(file.length()).isEqualTo(dataSize);

    }

    @Test(expected = ProxyCacheException.class)
    public void testErrorAppendFileCacheAfterCompletion() throws Exception {
        Cache fileCache = new FileCache(ProxyCacheTestUtils.newCacheFile());
        fileCache.append(ProxyCacheTestUtils.generate(20), 10);
        fileCache.complete();
        fileCache.append(ProxyCacheTestUtils.generate(20), 10);
        Assert.fail();
    }

    @Test
    public void testAppendDiscCache() throws Exception {
        File file = ProxyCacheTestUtils.newCacheFile();
        Cache fileCache = new FileCache(file);

        int firstPortionLength = 10000;
        byte[] firstDataPortion = ProxyCacheTestUtils.generate(firstPortionLength);
        fileCache.append(firstDataPortion, firstDataPortion.length);

        int secondPortionLength = 30000;
        byte[] secondDataPortion = ProxyCacheTestUtils.generate(secondPortionLength * 2);
        fileCache.append(secondDataPortion, secondPortionLength);

        byte[] wroteSecondPortion = Arrays.copyOfRange(secondDataPortion, 0, secondPortionLength);
        byte[] readData = new byte[secondPortionLength];
        fileCache.read(readData, firstPortionLength, secondPortionLength);
        assertThat(readData).isEqualTo(wroteSecondPortion);

        readData = new byte[fileCache.available()];
        fileCache.read(readData, 0, readData.length);
        byte[] fileContent = ProxyCacheTestUtils.getFileContent(ProxyCacheTestUtils.getTempFile(file));
        assertThat(readData).isEqualTo(fileContent);
    }

    @Test
    public void testIsFileCacheCompleted() throws Exception {
        File file = ProxyCacheTestUtils.newCacheFile();
        File partialFile = new File(file.getParentFile(), file.getName() + ".download");
        IoUtils.saveToFile(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_NAME), partialFile);
        Cache fileCache = new FileCache(partialFile);

        assertThat(file.exists()).isFalse();
        assertThat(partialFile.exists()).isTrue();
        assertThat(fileCache.isCompleted()).isFalse();

        fileCache.complete();

        assertThat(file.exists()).isTrue();
        assertThat(partialFile.exists()).isFalse();
        assertThat(fileCache.isCompleted()).isTrue();
        assertThat(partialFile.exists()).isFalse();
        assertThat(new FileCache(file).isCompleted()).isTrue();
    }

    @Test(expected = ProxyCacheException.class)
    public void testErrorWritingCompletedCache() throws Exception {
        File file = ProxyCacheTestUtils.newCacheFile();
        IoUtils.saveToFile(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_NAME), file);
        FileCache fileCache = new FileCache(file);
        fileCache.append(ProxyCacheTestUtils.generate(100), 20);
        Assert.fail();
    }

    @Test(expected = ProxyCacheException.class)
    public void testErrorWritingAfterCompletion() throws Exception {
        File file = ProxyCacheTestUtils.newCacheFile();
        File partialFile = new File(file.getParentFile(), file.getName() + ".download");
        IoUtils.saveToFile(ProxyCacheTestUtils.loadAssetFile(ProxyCacheTestUtils.ASSETS_DATA_NAME), partialFile);
        FileCache fileCache = new FileCache(partialFile);
        fileCache.complete();
        fileCache.append(ProxyCacheTestUtils.generate(100), 20);
        Assert.fail();
    }

    @Ignore("How to emulate file error?")
    @Test(expected = ProxyCacheException.class)
    public void testFileErrorForDiscCache() throws Exception {
        File file = new File("/system/data.bin");
        FileCache fileCache = new FileCache(file);
        Files.delete(file);
        fileCache.available();
        Assert.fail();
    }


}
