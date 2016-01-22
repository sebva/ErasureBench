package ch.unine.vauchers.erasuretester;

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
}
