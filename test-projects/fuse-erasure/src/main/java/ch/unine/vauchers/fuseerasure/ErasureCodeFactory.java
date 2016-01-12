package ch.unine.vauchers.fuseerasure;

import ch.unine.vauchers.fuseerasure.codes.ErasureCode;
import ch.unine.vauchers.fuseerasure.codes.ReedSolomonCode;

public class ErasureCodeFactory {
    private static ErasureCode erasureCode;

    public static ErasureCode getErasureCode() {
        if (erasureCode == null) {
            erasureCode = new ReedSolomonCode(10, 4);
        }

        return erasureCode;
    }
}
