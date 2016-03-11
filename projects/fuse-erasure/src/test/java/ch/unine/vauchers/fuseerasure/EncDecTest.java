package ch.unine.vauchers.fuseerasure;

import ch.unine.vauchers.fuseerasure.codes.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class EncDecTest {

    @Test
    public void testComputeDataSize() {
        EncDec encdec = new EncDec(new ReedSolomonCode(10, 4));

        Assert.assertEquals(0, encdec.computeDataSize(0));
        Assert.assertEquals(14, encdec.computeDataSize(1));
        Assert.assertEquals(14, encdec.computeDataSize(9));
        Assert.assertEquals(14, encdec.computeDataSize(10));
        Assert.assertEquals(28, encdec.computeDataSize(11));
        Assert.assertEquals(28, encdec.computeDataSize(20));
        Assert.assertEquals(42, encdec.computeDataSize(21));
    }

    @Test
    public void testEncodeDecode() throws UnsupportedEncodingException {
        ErasureCode[] codes = new ErasureCode[] {
                new ReedSolomonCode(10, 4),
                new XORCode(2, 1),
                new SimpleRegeneratingCode(10, 6)
        };
        EncDec.ErasureGenerator[] erasureGenerators = new EncDec.ErasureGenerator[] {
                (paritySize, stripeSize) -> new int[0],
                (paritySize, stripeSize) -> new int[] {(int) (Math.random() * (paritySize + stripeSize))},
                (paritySize, stripeSize) -> new int[] {2, 4, 5, 10},
                (paritySize, stripeSize) -> new int[] {0, 1, 2, 3},
                (paritySize, stripeSize) -> new int[] {10, 11, 12, 13}
        };

        for (ErasureCode code : codes) {
            for (EncDec.ErasureGenerator erasureGenerator : erasureGenerators) {
                EncDec encdec = new EncDec(code, (code instanceof XORCode ? erasureGenerators[1] : erasureGenerator));
                byte[] test = "This is a test message!".getBytes("UTF-8");
                encdec.storeContents(ByteBuffer.wrap(test));
                ByteBuffer results = encdec.restoreContents();
                Assert.assertArrayEquals(test, results.array());
            }
        }
    }

}
