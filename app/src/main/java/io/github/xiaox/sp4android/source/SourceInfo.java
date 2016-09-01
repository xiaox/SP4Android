package io.github.xiaox.sp4android.source;

/**
 * @author X
 * @version V0.1.0
 */
public class SourceInfo {
    public final String url;
    public final int length;
    public final String mime;

    public SourceInfo(String url, int length, String mime) {
        this.url = url;
        this.length = length;
        this.mime = mime;
    }
}
