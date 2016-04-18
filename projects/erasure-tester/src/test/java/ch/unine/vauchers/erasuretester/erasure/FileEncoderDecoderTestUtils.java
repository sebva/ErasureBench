package ch.unine.vauchers.erasuretester.erasure;

import java.math.BigInteger;
import java.util.Random;

/**
 *
 */
final class FileEncoderDecoderTestUtils {

    final static Random random = new Random(8543925432L);

    static String generateRandomPath() {
        return new BigInteger(80, random).toString(32);
    }

    static byte[] createRandomBigByteBuffer() {
        final int size = 24000;
        final byte[] byteBuffer = new byte[size];
        random.nextBytes(byteBuffer);
        return byteBuffer;
    }
}
