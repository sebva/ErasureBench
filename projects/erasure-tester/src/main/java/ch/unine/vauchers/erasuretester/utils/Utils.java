package ch.unine.vauchers.erasuretester.utils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.logging.LogManager;

/**
 * Collection of useful static methods
 */
public class Utils {
    /**
     * Globally disable logging
     */
    public static void disableLogging() {
        LogManager.getLogManager().reset();
    }

    @NotNull
    public static Object getPrivateField(Class<?> clazz, Object object, String fieldName) throws NoSuchFieldException {
        final Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return new Object();
        }
    }
}
