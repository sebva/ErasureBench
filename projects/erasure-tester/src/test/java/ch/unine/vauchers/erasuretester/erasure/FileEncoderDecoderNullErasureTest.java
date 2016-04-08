package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.NullErasureCode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class FileEncoderDecoderNullErasureTest extends FileEncoderDecoderTest {

    private FileEncoderDecoder sut;

    @Override
    protected Iterable<FileEncoderDecoder> createEncoderDecoder() {
        sut = new FileEncoderDecoder(new NullErasureCode(10), new MemoryStorageBackend());
        return Collections.singleton(sut);
    }

    @Test
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

    @Test
    public void testLowerBytesToDrop() {
        IntStream.rangeClosed(0, 9).forEach((index) -> assertEquals(index, sut.lowerBytesToDrop(index)));
        IntStream.rangeClosed(10, 19).forEach((index) -> assertEquals(index - 10, sut.lowerBytesToDrop(index)));
        IntStream.rangeClosed(20, 29).forEach((index) -> assertEquals(index - 20, sut.lowerBytesToDrop(index)));
        IntStream.rangeClosed(30, 39).forEach((index) -> assertEquals(index - 30, sut.lowerBytesToDrop(index)));
    }

    @SuppressWarnings("Duplicates")
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
