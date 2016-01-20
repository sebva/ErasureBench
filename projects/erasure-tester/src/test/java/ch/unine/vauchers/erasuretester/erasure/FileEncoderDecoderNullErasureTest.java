package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.erasure.codes.NullErasureCode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileEncoderDecoderNullErasureTest extends FileEncoderDecoderTest {
    @Override
    @Before
    public void setup() {
        sut = createEncoderDecoder(new NullErasureCode(10));
    }

    @Test
    @Override
    public void testComputeDataSize() {
        Assert.assertEquals(0, sut.nextBoundary(0));
        Assert.assertEquals(10, sut.nextBoundary(1));
        Assert.assertEquals(10, sut.nextBoundary(9));
        Assert.assertEquals(10, sut.nextBoundary(10));
        Assert.assertEquals(20, sut.nextBoundary(11));
        Assert.assertEquals(20, sut.nextBoundary(20));
        Assert.assertEquals(30, sut.nextBoundary(21));
    }

    @Test
    @Override
    public void testPreviousBoundary() {
        assertEquals(0, sut.previousBoundary(0));
        assertEquals(0, sut.previousBoundary(1));
        assertEquals(0, sut.previousBoundary(9));
        assertEquals(10, sut.previousBoundary(10));
        assertEquals(10, sut.previousBoundary(11));
        assertEquals(10, sut.previousBoundary(13));
        assertEquals(10, sut.previousBoundary(14));
        assertEquals(10, sut.previousBoundary(19));
        assertEquals(20, sut.previousBoundary(20));
        assertEquals(20, sut.previousBoundary(29));
        assertEquals(30, sut.previousBoundary(30));
        assertEquals(30, sut.previousBoundary(34));
    }
}
