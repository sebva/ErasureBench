package ch.unine.vauchers.erasuretester.backend;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public abstract class StorageBackendTest<T extends StorageBackend> {
    private T sut;
    private static final Random random = new Random(3294328756L);

    protected abstract T createInstance();

    @Before
    public void setup() {
        sut = createInstance();
        sut.defineTotalSize(2);
    }

    @After
    public void tearDown() {
        sut.disconnect();
    }

    @Test
    public void testReadWriteSync() {
        testReadWrite(sut::storeBlock, (key) -> sut.retrieveBlock(key).get());
    }

    @Test
    public void testAbsentKey() throws ExecutionException, InterruptedException {
        assertFalse(sut.isBlockAvailable(439754395));
    }

    private void testReadWrite(BiFunction<Integer, Integer, Integer> storeFunction, Function<Integer, Integer> retrieveFunction) {
        final int testSize = 4 * sut.bufferSize;
        List<Integer> keys = new ArrayList<>(testSize);
        List<Integer> values = new ArrayList<>(testSize);
        for (int i = 0; i < testSize; i++) {
            values.add(random.nextInt());
        }

        values.stream().forEachOrdered(value -> keys.add(storeFunction.apply(value, Math.abs(value % 2))));

        sut.flushAll();

        IntStream.range(0, testSize).forEach(i -> assertEquals(values.get(i), retrieveFunction.apply(keys.get(i))));
    }

    @Test
    public void testFileMetadataStorage() {
        FileMetadata expected = new FileMetadata();
        IntList blockKeys = new IntArrayList(30);
        for (int i = 0; i < 30; i++) {
            blockKeys.add((int) (Math.random() * (double) Long.MAX_VALUE));
        }

        expected.setContentsSize(291643824);
        expected.setBlockKeys(blockKeys);
        sut.setFileMetadata("somewhere", expected);

        FileMetadata actual = sut.getFileMetadata("somewhere").get();
        assertEquals(291643824, actual.getContentsSize());

        final IntListIterator iterator1 = blockKeys.iterator();
        final IntList actualBlocks = actual.getBlockKeys().get();
        final IntListIterator iterator2 = actualBlocks.iterator();

        while (iterator1.hasNext() && iterator2.hasNext()) {
            assertEquals(iterator1.next(), iterator2.next());
        }

        assertEquals(blockKeys.size(), actualBlocks.size());
    }

    @Test
    public void testComputePositionWithBlockKey() {
        sut.defineTotalSize(14);

        for (int i = 0; i < 2000000; i++) {
            final int position = random.nextInt(14);
            final int blockKey = sut.storeBlock(0, position);
            assertEquals("Key=" + blockKey, position, sut.computePositionWithBlockKey(blockKey));
        }
    }

    @Test
    public void testComputePositionWithRedisKey() {
        sut.defineTotalSize(14);

        for (int i = 0; i < 2000000; i++) {
            final int position = random.nextInt(14);
            final int blockKey = sut.storeBlock(0, position);
            final int redisKey = blockKey / sut.bufferSize;
            assertEquals("Key=" + blockKey, position, sut.computePositionWithRedisKey(redisKey));
        }
    }
}
