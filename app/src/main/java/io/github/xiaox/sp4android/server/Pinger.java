package io.github.xiaox.sp4android.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.github.xiaox.sp4android.exception.ProxyCacheException;
import io.github.xiaox.sp4android.source.HttpUrlSource;
import io.github.xiaox.sp4android.util.Preconditions;

/**
 * @author X
 * @version 0.1.0
 */

public class Pinger {

    private final String PING_REQUEST = "ping";
    private final String PING_RESPONSE = "ping ok";

    private final ExecutorService pingExecutor = Executors.newSingleThreadExecutor();
    private final String host;
    private final int port;

    Pinger(String host, int port) {
        this.host = host;
        this.port = port;
    }

    boolean ping(int maxAttempts, int startTimeout) {
        Preconditions.checkArgument(maxAttempts >= 1);
        Preconditions.checkArgument(startTimeout >= 0);
        int timeout = startTimeout;
        int attempts = 0;

        while (attempts < maxAttempts) {
            Future<Boolean> pingFuture = pingExecutor.submit(new PingCallable());
            try {
                boolean pinged = pingFuture.get(timeout, TimeUnit.MILLISECONDS);
                if (pinged) return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            attempts++;
            timeout *= 2;
        }
        return false;
    }

    void responseToPing(Socket socket) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write("HTTP/1.1 200 OK\n\n".getBytes());
        out.write(PING_RESPONSE.getBytes());
    }

    public boolean isPingRequest(String request) {
        return PING_REQUEST.equals(request);
    }

    private class PingCallable implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            return pingServer();
        }
    }

    private boolean pingServer() throws ProxyCacheException {
        String pingUrl = getPingUrl();
        HttpUrlSource source = new HttpUrlSource(pingUrl);

        try {
            byte[] expectedResponse = PING_RESPONSE.getBytes();
            source.open(0);

            byte[] response = new byte[expectedResponse.length];
            source.read(response);
            boolean pingOK = Arrays.equals(expectedResponse, response);
            return pingOK;

        } catch (ProxyCacheException e) {
            return false;
        } finally {
            source.close();
        }

    }

    private String getPingUrl() {
        return String.format(Locale.US, "http://%s:%d/%s", host, port, PING_REQUEST);
    }

}
