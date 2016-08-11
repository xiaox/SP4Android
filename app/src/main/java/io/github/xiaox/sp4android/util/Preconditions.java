package io.github.xiaox.sp4android.util;

/**
 * @author X
 * @version V0.1.0
 */
public final class Preconditions {

    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static <T> T checkNotNull(T referecne, String errorMessage) {
        if (referecne == null) {
            throw new NullPointerException(errorMessage);
        }
        return referecne;
    }

    public static void checkAllNotNull(Object... references) {
        for (Object obj : references) {
            if (obj == null) {
                throw new NullPointerException();
            }
        }
    }

    public static void checkArgument(boolean expression) {
        if(!expression) {
            throw  new IllegalArgumentException();
        }
    }

    public static void checkArgument(boolean expression, String errorMessage) {
        if(!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
