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

    /**
     * Access the private field of an object
     * @param clazz The class of the object
     * @param object The object itself
     * @param fieldName The field to access
     * @return The value of the field, as an Object
     * @throws NoSuchFieldException
     */
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
