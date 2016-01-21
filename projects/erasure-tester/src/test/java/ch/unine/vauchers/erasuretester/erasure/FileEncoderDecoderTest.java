package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.ReedSolomonCode;
import ch.unine.vauchers.erasuretester.erasure.codes.TooManyErasedLocations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class FileEncoderDecoderTest {
    protected FileEncoderDecoder sut;

    protected FileEncoderDecoder createEncoderDecoder(ErasureCode erasureCode) {
        return new FileEncoderDecoder(erasureCode, new MemoryStorageBackend());
    }

    @Before
    public void setup() {
        sut = createEncoderDecoder(new ReedSolomonCode(10, 4));
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
        byteBuffer.put(size -1, (byte) 42);
        byteBuffer.rewind();
        return byteBuffer;
    }

    @Test
    public void testComputeDataSize() {
        assertEquals(0, sut.nextBoundary(0));
        assertEquals(14, sut.nextBoundary(1));
        assertEquals(14, sut.nextBoundary(9));
        assertEquals(14, sut.nextBoundary(10));
        assertEquals(28, sut.nextBoundary(11));
        assertEquals(28, sut.nextBoundary(20));
        assertEquals(42, sut.nextBoundary(21));
        assertEquals(68824, sut.nextBoundary(45056 + 4096));
    }

    @Test
    public void testPreviousBoundary() {
        assertEquals(0, sut.previousBoundary(0));
        assertEquals(0, sut.previousBoundary(1));
        assertEquals(0, sut.previousBoundary(9));
        assertEquals(14, sut.previousBoundary(10));
        assertEquals(14, sut.previousBoundary(11));
        assertEquals(14, sut.previousBoundary(13));
        assertEquals(14, sut.previousBoundary(14));
        assertEquals(14, sut.previousBoundary(19));
        assertEquals(28, sut.previousBoundary(20));
        assertEquals(28, sut.previousBoundary(29));
        assertEquals(42, sut.previousBoundary(30));
        assertEquals(42, sut.previousBoundary(34));
        assertEquals(63070, sut.previousBoundary(45056));
    }

    @Test
    public void testLowerBytesToDrop() {
        IntStream.rangeClosed(0, 9).forEach((index) -> assertEquals(index, sut.lowerBytesToDrop(index)));
        IntStream.rangeClosed(10, 19).forEach((index) -> assertEquals(index - 10, sut.lowerBytesToDrop(index)));
        IntStream.rangeClosed(20, 29).forEach((index) -> assertEquals(index - 20, sut.lowerBytesToDrop(index)));
        IntStream.rangeClosed(30, 39).forEach((index) -> assertEquals(index - 30, sut.lowerBytesToDrop(index)));
    }

    @Test
    public void testHigherBytesToDrop() {
        assertEquals(10, sut.higherBytesToDrop(0));
        assertEquals(9, sut.higherBytesToDrop(1));
        assertEquals(1, sut.higherBytesToDrop(9));
        assertEquals(0, sut.higherBytesToDrop(10));
        assertEquals(9, sut.higherBytesToDrop(11));
        assertEquals(1, sut.higherBytesToDrop(19));
        assertEquals(0, sut.higherBytesToDrop(20));
    }
}
