package nl.ellipsis.webdav.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
Reference: https://github.com/ellipsisnl/PfxWebDAV/tree/master/PfxWebDAVAPI
 */
public class StatusEnum<T> {

    private final T t;
    private final String delimiter = ";";

    public final int code;
    public final String message;

    public static <T> StatusEnum<T> set(T t) {
        return new StatusEnum<T>(t);
    }

    private StatusEnum(T t) {
        this.t = t;
        String value = getValueByReflection();
        if (value != null) {
            int p = value.lastIndexOf(delimiter);
            if (p > 0) {
                this.code = Integer.parseInt(value.substring(p + 1).trim());
                this.message = value.substring(0, p);
            } else {
                this.code = -1;
                this.message = value;
            }
        } else {
            this.code = -1;
            this.message = t.toString();
        }
    }

    private String getValueByReflection() {
        if (this.t instanceof Enum<?>) {
            try {
                Method m = this.t.getClass().getMethod("value");
                if (m != null) {
                    return (String) m.invoke(this.t);
                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                // e.printStackTrace();
            }
        }
        return null;
    }

}
