package ch.unine.vauchers.fuseerasure;

import ch.unine.vauchers.fuseerasure.ErasureCodeFactory;
import ch.unine.vauchers.fuseerasure.codes.ErasureCode;
import ch.unine.vauchers.fuseerasure.codes.XORCode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ErasureCodeTest {
    
    @Test
    public void testXorEncodeDecode() {
        ErasureCode code = new XORCode(2, 1);

        int[] message = new int[]{57, 42};
        int[] parity = new int[1];
        code.encode(message, parity);

        Assert.assertEquals(parity[0], 19);
        System.out.println(message[0] + ", " + message[1] + ", " + parity[0]);

        // Try to restore message[1]
        int[] out = new int[1];
        int[] data = {parity[0], message[0], -1274};
        code.decode(data, new int[]{2}, out);
        System.out.println(Arrays.toString(out));

        Assert.assertEquals(message[1], out[0]);
    }
}
