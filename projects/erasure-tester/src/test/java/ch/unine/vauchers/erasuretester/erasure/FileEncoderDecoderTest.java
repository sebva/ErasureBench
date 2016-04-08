package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.erasure.codes.TooManyErasedLocations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public abstract class FileEncoderDecoderTest {
    protected Iterable<FileEncoderDecoder> suts;
    private Random random;

    protected abstract Iterable<FileEncoderDecoder> createEncoderDecoder();

    @Before
    public void setup() {
        random = new Random(8543925432L);
        suts = createEncoderDecoder();
    }

    @Test
    public void testBasic() throws TooManyErasedLocations, UnsupportedEncodingException {
        for (FileEncoderDecoder sut : suts) {
            byte[] test = "This is a test message!".getBytes("UTF-8");
            final String path = "path";
            sut.writeFile(path, test.length, 0, ByteBuffer.wrap(test));

            ByteBuffer results = ByteBuffer.allocate(test.length);
            sut.readFile(path, test.length, 0, results);
            Assert.assertArrayEquals(test, results.array());
        }
    }

    @Test
    public void testAlignedFile() throws TooManyErasedLocations {
        testUnalignedFile(23000, 500);
    }

    @Test
    public void testUnalignedFileBeginEnd() throws TooManyErasedLocations {
        testUnalignedFile(23000, 502);
    }

    @Test
    public void testUnalignedFileBegin() throws TooManyErasedLocations {
        testUnalignedFile(22998, 502);
    }

    @Test
    public void testUnalignedFileEnd() throws TooManyErasedLocations {
        testUnalignedFile(23015, 500);
    }

    @Test
    public void test10bytesFile() throws TooManyErasedLocations {
        final byte[] array = new byte[10];
        random.nextBytes(array);
        testUnalignedFile(10, 0, array);
    }

    private void testUnalignedFile(int size, int offset) throws TooManyErasedLocations {
        testUnalignedFile(size, offset, createRandomBigByteBuffer());
    }

    private void testUnalignedFile(int size, int offset, byte[] inBuffer) throws TooManyErasedLocations {
        for (FileEncoderDecoder sut : suts) {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(inBuffer);
            final String path = generateRandomPath();

            sut.writeFile(path, size, offset, byteBuffer);
            final ByteBuffer byteBufferOut = ByteBuffer.allocate(byteBuffer.capacity());
            sut.readFile(path, size, offset, byteBufferOut);

            byteBuffer.rewind();
            byteBufferOut.rewind();

            for (int i = 0; i < size; i++) {
                assertEquals("Inequality at index " + byteBuffer.position() + "\nEncoderDecoder: " + sut.toString(), byteBuffer.get(), byteBufferOut.get());
            }
        }
    }

    private String generateRandomPath() {
        return new BigInteger(80, random).toString(32);
    }

    private byte[] createRandomBigByteBuffer() {
        final int size = 24000;
        final byte[] byteBuffer = new byte[size];
        random.nextBytes(byteBuffer);
        return byteBuffer;
    }
}
