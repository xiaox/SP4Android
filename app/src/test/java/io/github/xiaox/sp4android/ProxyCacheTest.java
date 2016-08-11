package io.github.xiaox.sp4android;

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import io.github.xiaox.sp4android.cache.ByteArrayCache;
import io.github.xiaox.sp4android.exception.ProxyCacheException;
import io.github.xiaox.sp4android.proxy.ProxyCache;
import io.github.xiaox.sp4android.source.ByteArraySource;

import static org.fest.assertions.api.Assertions.assertThat;


/**
 * @author X
 * @version V0.1.0
 */
public class ProxyCacheTest {

    @Test
    public void testNoCache() throws ProxyCacheException {
        byte[] sourceData = generate(234);
        ProxyCache proxyCache = new ProxyCache(new ByteArraySource(sourceData), new ByteArrayCache());

        byte[] buffer = new byte[sourceData.length];
        int read = proxyCache.read(buffer, 0, sourceData.length);

        assertThat(read).isEqualTo(sourceData.length);
        assertThat(buffer).isEqualTo(sourceData);
    }



    @Test
    public void testAllFromCacheNoSource() throws ProxyCacheException {
        byte[] cacheData = generate(3456);
        ProxyCache proxyCache = new ProxyCache(new ByteArraySource(new byte[0]), new ByteArrayCache(cacheData));

        byte[] buffer = new byte[cacheData.length + 100];
        int read = proxyCache.read(buffer, 0, cacheData.length);
        byte[] readData = Arrays.copyOfRange(buffer, 0, cacheData.length);

        assertThat(read).isEqualTo(cacheData.length);
        assertThat(readData).isEqualTo(cacheData);

    }

    public byte[] generate(int capacity) {
        Random random = new Random(System.currentTimeMillis());
        byte[] result = new byte[capacity];
        random.nextBytes(result);
        return result;
    }
}
