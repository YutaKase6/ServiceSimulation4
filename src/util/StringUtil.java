package util;

/**
 * Created by yutakase on 2016/12/11.
 */
public final class StringUtil {
    private StringUtil() {
    }

    public static String formatTo1f(double num) {
        return String.format("%1$.1f", num);
    }
}
