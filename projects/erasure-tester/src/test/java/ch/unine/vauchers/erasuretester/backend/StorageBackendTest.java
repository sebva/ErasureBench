package ch.unine.vauchers.erasuretester.backend;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

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
    public void testReadWriteAsync() {
        testReadWrite((key, blockData) -> {
            final Future<Boolean> future = sut.storeBlockAsync(key, blockData);
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                fail(e.getMessage());
            }
        }, (key) -> {
            try {
                return sut.retrieveBlockAsync(key).get();
            } catch (InterruptedException | ExecutionException e) {
                fail(e.getMessage());
                return null;
            }
        });
    }

    @Test
    public void testBulkRetrieveAsync() throws ExecutionException, InterruptedException {
        final Random rnd = new Random();
        Map<Long, Integer> expected = new HashMap<>();
        int[] values = {42, -122, 64, 144, 0, 1, -1};

        Arrays.stream(values).forEach(value -> expected.put(new BigInteger(40, rnd).longValue(), value));

        expected.forEach(sut::storeBlock);

        final Map<Long, Integer> actual = sut.retrieveAllBlocksAsync(expected.keySet()).get();

        expected.forEach((key, value) -> assertEquals(value, actual.get(key)));
    }

    @Test
    public void testAbsentKey() throws ExecutionException, InterruptedException {
        assertFalse(sut.isBlockAvailableAsync(32134214L).get());
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
}
