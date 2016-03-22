package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.erasure.codes.TooManyErasedLocations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public abstract class FileEncoderDecoderTest {
    protected FileEncoderDecoder sut;

    protected abstract FileEncoderDecoder createEncoderDecoder();

    @Before
    public void setup() {
        sut = createEncoderDecoder();
    }

    @Test
    public void testBasic() throws TooManyErasedLocations, UnsupportedEncodingException {
        byte[] test = "This is a test message!".getBytes("UTF-8");
        final String path = "path";
        sut.writeFile(path, test.length, 0, ByteBuffer.wrap(test));

        ByteBuffer results = ByteBuffer.allocate(test.length);
        sut.readFile(path, test.length, 0, results);
        Assert.assertArrayEquals(test, results.array());
    }

    @Test
    public void testAlignedFile() throws TooManyErasedLocations {
        testUnalignedFile(23000, 500, Optional.empty());
    }

    @Test
    public void testRealLifeFile() throws TooManyErasedLocations {
        testUnalignedFile(4096, 16384, Optional.of(ByteBuffer.allocateDirect(4096)));
    }

    @Test
    public void testUnalignedFileBeginEnd() throws TooManyErasedLocations {
        testUnalignedFile(23000, 502, Optional.empty());
    }

    @Test
    public void testUnalignedFileBegin() throws TooManyErasedLocations {
        testUnalignedFile(22998, 502, Optional.empty());
    }

    @Test
    public void testUnalignedFileEnd() throws TooManyErasedLocations {
        testUnalignedFile(23015, 500, Optional.empty());
    }

    private void testUnalignedFile(int size, int offset, Optional<ByteBuffer> inBuffer) throws TooManyErasedLocations {
        final ByteBuffer byteBuffer = inBuffer.orElseGet(FileEncoderDecoderTest::createRandomBigByteBuffer);

        sut.writeFile("path", size, offset, byteBuffer);
        final ByteBuffer byteBufferOut = ByteBuffer.allocate(byteBuffer.capacity());
        sut.readFile("path", size, offset, byteBufferOut);

        byteBuffer.rewind();
        byteBufferOut.rewind();

        for (int i = 0; i < size; i++) {
            assertEquals("Inequality at index " + byteBuffer.position(), byteBuffer.get(), byteBufferOut.get());
        }
    }

    private static ByteBuffer createRandomBigByteBuffer() {
        final int size = 24000;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        final Random random = new Random();
        for (int i = 0; i < size / Integer.BYTES; i++) {
            byteBuffer.putInt(random.nextInt());
        }
        byteBuffer.put(size - 1, (byte) 42);
        byteBuffer.rewind();
        return byteBuffer;
    }
}
