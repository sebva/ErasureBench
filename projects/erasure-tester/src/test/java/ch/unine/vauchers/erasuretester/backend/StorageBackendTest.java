package ch.unine.vauchers.erasuretester.backend;

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public abstract class StorageBackendTest<T extends StorageBackend> {
    private T sut;

    protected abstract T createInstance();

    @Before
    public void setup() {
        sut = createInstance();
    }

    @Test
    public void testReadWrite() {
        final Random rnd = new Random();
        String key1 = new BigInteger(40, rnd).toString(256);
        String key2 = new BigInteger(40, rnd).toString(256);

        int value1 = 42;
        int value2 = -122;
        sut.storeBlock(key1, value1);
        sut.storeBlock(key2, value2);

        assertEquals(value1, (int) sut.retrieveBlock(key1).get());
        assertEquals(value2, (int) sut.retrieveBlock(key2).get());

        sut.storeBlock(key1, value2);
        assertEquals(value2, (int) sut.retrieveBlock(key1).get());
    }

    protected FailureGenerator createNullFailureGenerator() {
        return new NullFailureGenerator();
    }
}
