package ch.unine.vauchers.erasuretester;

import java.util.logging.LogManager;

/**
 *
 */
public class Utils {
    public static void disableLogging() {
        LogManager.getLogManager().reset();
    }
}
