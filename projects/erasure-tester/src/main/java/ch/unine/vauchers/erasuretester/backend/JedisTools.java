package ch.unine.vauchers.erasuretester.backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class JedisTools {
    static String[] CRC16_NUMBERS_CORRESPONDANCES;
    public static final int REDIS_KEYS_NUMBER = 16384;

    static synchronized void initialize() {
        if (CRC16_NUMBERS_CORRESPONDANCES != null) {
            return;
        }

        final BufferedReader input = new BufferedReader(
                new InputStreamReader(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("crc-map.txt")
                ));
        final List<String> list = new ArrayList<>(REDIS_KEYS_NUMBER);
        String line = null;
        try {
            while ((line = input.readLine()) != null) {
                list.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assert list.size() == REDIS_KEYS_NUMBER;
        CRC16_NUMBERS_CORRESPONDANCES = new String[REDIS_KEYS_NUMBER];
        CRC16_NUMBERS_CORRESPONDANCES = list.toArray(CRC16_NUMBERS_CORRESPONDANCES);
    }
}
