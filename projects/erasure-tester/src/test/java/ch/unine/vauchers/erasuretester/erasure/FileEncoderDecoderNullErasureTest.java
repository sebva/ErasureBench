package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.erasure.codes.NullErasureCode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FileEncoderDecoderNullErasureTest extends FileEncoderDecoderTest {
    @Override
    @Before
    public void setup() {
        sut = createEncoderDecoder(key -> false, new NullErasureCode(10));
    }

    @Test
    public void testComputeDataSize() {
        Assert.assertEquals(0, sut.computeDataSize(0));
        Assert.assertEquals(10, sut.computeDataSize(1));
        Assert.assertEquals(10, sut.computeDataSize(9));
        Assert.assertEquals(10, sut.computeDataSize(10));
        Assert.assertEquals(20, sut.computeDataSize(11));
        Assert.assertEquals(20, sut.computeDataSize(20));
        Assert.assertEquals(30, sut.computeDataSize(21));
    }
}
