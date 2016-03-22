package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.ReedSolomonCode;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class FileEncoderDecoderReedSolomonErasureTest extends FileEncoderDecoderTest {
    @Override
    protected FileEncoderDecoder createEncoderDecoder() {
        return new FileEncoderDecoder(new ReedSolomonCode(10, 4), new MemoryStorageBackend());
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
