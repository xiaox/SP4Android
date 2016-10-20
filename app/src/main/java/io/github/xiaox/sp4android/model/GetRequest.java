package io.github.xiaox.sp4android.model;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.xiaox.sp4android.util.Preconditions;

/**
 * @author X
 * @version V0.1.0
 */
public class GetRequest {

    private static final Pattern RANGE_HEADER_PATTERN = Pattern.compile("[R,r]ange:[ ]?bytes=(\\d*)-");
    private static final Pattern URL_PATTERN = Pattern.compile("GET /(.*) HTTP");

    public final String uri;
    public final long rangeOffset;
    public final boolean partial;

    public GetRequest(String request) {
        Preconditions.checkNotNull(request);
        long offset = findRangeOffset(request);
        this.rangeOffset = Math.max(0, offset);
        this.partial = offset >= 0;
        this.uri = findUri(request);
    }

    public static GetRequest read(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while (!TextUtils.isEmpty(line = reader.readLine())) {
            stringBuilder.append(line).append("\n");
        }
        return new GetRequest(stringBuilder.toString());
    }

    private long findRangeOffset(String request) {
        Matcher matcher = RANGE_HEADER_PATTERN.matcher(request);
        if (matcher.find()) {
            String rangeValue = matcher.group(1);
            return Long.parseLong(rangeValue);
        }
        return -1;
    }

    private String findUri(String request) {
        Matcher matcher = URL_PATTERN.matcher(request);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Invalid request `" + request + "`: url not found!");
    }

}
