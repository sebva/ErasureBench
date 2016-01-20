package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.FailureGenerator;
import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.ReedSolomonCode;
import ch.unine.vauchers.erasuretester.erasure.codes.TooManyErasedLocations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class FileEncoderDecoderTest {
    protected FileEncoderDecoder sut;

    protected FileEncoderDecoder createEncoderDecoder(FailureGenerator failureGenerator, ErasureCode erasureCode) {
        return new FileEncoderDecoder(erasureCode, new MemoryStorageBackend(failureGenerator));
    }

    @Before
    public void setup() {
        sut = createEncoderDecoder(key -> false, new ReedSolomonCode(10, 4));
    }

    @Test
    public void testBasic() throws TooManyErasedLocations, UnsupportedEncodingException {
        byte[] test = "This is a test message!".getBytes("UTF-8");
        final String path = "path";
        sut.writeFile(path, ByteBuffer.wrap(test));
        ByteBuffer results = sut.readFile(path);
        Assert.assertArrayEquals(test, results.array());
    }

    @Test
    public void testComputeDataSize() {
        Assert.assertEquals(0, sut.computeDataSize(0));
        Assert.assertEquals(14, sut.computeDataSize(1));
        Assert.assertEquals(14, sut.computeDataSize(9));
        Assert.assertEquals(14, sut.computeDataSize(10));
        Assert.assertEquals(28, sut.computeDataSize(11));
        Assert.assertEquals(28, sut.computeDataSize(20));
        Assert.assertEquals(42, sut.computeDataSize(21));
    }
}
