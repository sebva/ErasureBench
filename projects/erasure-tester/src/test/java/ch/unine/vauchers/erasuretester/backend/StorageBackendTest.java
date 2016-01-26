package ch.unine.vauchers.erasuretester.backend;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public abstract class StorageBackendTest<T extends StorageBackend> {
    private T sut;

    protected abstract T createInstance();

    @Before
    public void setup() {
        sut = createInstance();
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
        assertFalse(sut.isBlockAvailable(32432134214L));
    }

    private static void testReadWrite(BiConsumer<Long, Integer> storeFunction, Function<Long, Integer> retrieveFunction) {
        final Random rnd = new Random();
        long key1 = new BigInteger(40, rnd).longValue();
        long key2 = new BigInteger(40, rnd).longValue();

        int value1 = 42;
        int value2 = -122;

        storeFunction.accept(key1, value1);
        storeFunction.accept(key2, value2);

        assertEquals(value1, (int) retrieveFunction.apply(key1));
        assertEquals(value2, (int) retrieveFunction.apply(key2));

        storeFunction.accept(key1, value2);
        assertEquals(value2, (int) retrieveFunction.apply(key1));
    }

    @Test
    public void testFileMetadataStorage() {
        FileMetadata expected = new FileMetadata();
        List<Long> blockKeys = new ArrayList<>(30);
        for (int i = 0; i < 30; i++) {
            blockKeys.add((long) (Math.random() * (double) Long.MAX_VALUE));
        }

        expected.setContentsSize(291643824);
        expected.setBlockKeys(blockKeys);
        sut.setFileMetadata("somewhere", expected);

        FileMetadata actual = sut.getFileMetadata("somewhere").get();
        assertEquals(291643824, actual.getContentsSize());

        final Iterator<Long> iterator1 = blockKeys.iterator();
        final List<Long> actualBlocks = actual.getBlockKeys().get();
        final Iterator<Long> iterator2 = actualBlocks.iterator();

        while (iterator1.hasNext() && iterator2.hasNext()) {
            assertEquals(iterator1.next(), iterator2.next());
        }

        assertEquals(blockKeys.size(), actualBlocks.size());
    }
}
