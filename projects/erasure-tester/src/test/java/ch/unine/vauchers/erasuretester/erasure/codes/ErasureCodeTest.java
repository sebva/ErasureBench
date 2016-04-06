package ch.unine.vauchers.erasuretester.erasure.codes;

import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
public abstract class ErasureCodeTest<T extends ErasureCode> {
    private T sut;
    private Random random;

    public abstract int getStripeSize();

    public abstract int getParitySize();

    public abstract int getMaxErasures();

    protected abstract T newSut();

    // data and data2 contain the same values. Use one for input and one for checking.
    private int[] data;
    private int[] data2;
    // Pre-instantiated output arrays
    private int[] stripe;
    private int[] parity;

    @Before
    public void setup() {
        this.sut = newSut();
        random = new Random();
        data = new int[getStripeSize()];
        data2 = new int[getStripeSize()];
        byte[] bytesData = new byte[getStripeSize()];
        random.nextBytes(bytesData);
        for (int i = 0; i < getStripeSize(); i++) {
            data[i] = data2[i] = Byte.toUnsignedInt(bytesData[i]);
        }
        parity = new int[getParitySize()];
    }

    @Test
    public void testNoErasure() {
        sut.encode(data, parity);
        checkData();
        sut.decode(data, new int[0], new int[0]);
        checkData();
    }

    @Test
    public void testWithErasures() {
        sut.encode(data, parity);
        for (int j = 0; j < 10000; j++) {
            for (int i = 0; i < getParitySize() + getStripeSize(); i++) {
                checkData();

                int[] mergedParityData = mergeArrays(parity, data);
                int[] erasures = generateErasures(i);

                for (int erasure : erasures) {
                    mergedParityData[erasure] = 0;
                }
                int[] dataErasures = Arrays.stream(erasures).filter(value -> value >= getParitySize()).toArray();


                final IntList locationsToReadForDecode;
                try {
                    locationsToReadForDecode = sut.locationsToReadForDecode(Arrays.stream(erasures).boxed().collect(Collectors.toList()));
                } catch (TooManyErasedLocations e) {
                    if (erasures.length <= getMaxErasures()) {
                        Assert.fail();
                        return;
                    } else {
                        break;
                    }
                }

                int[] recoveredValues = new int[dataErasures.length];
                sut.decode(mergedParityData, dataErasures, recoveredValues, locationsToReadForDecode.toIntArray(), erasures);
                restoreValues(data, erasures, recoveredValues);
                checkData(i);
            }
        }
    }

    private int[] mergeArrays(int[] parity, int[] data) {
        return IntStream.concat(Arrays.stream(parity), Arrays.stream(data)).toArray();
    }

    private void restoreValues(int[] data, int[] recoveredIndices, int[] recoveredValues) {
        final PrimitiveIterator.OfInt iterator = Arrays.stream(recoveredValues).iterator();
        Arrays.stream(recoveredIndices)
                .filter(value -> value >= getParitySize())
                .map(value -> value - getParitySize())
                .forEachOrdered(value -> data[value] = iterator.nextInt());
    }

    private void checkData(int i) {
        checkData();
        System.out.println(i + " erasures OK");
    }

    private void checkData() {
        Assert.assertArrayEquals(data2, data);
    }

    private int[] generateErasures(int amount) {
        return random.ints(amount * 2, 0, getParitySize() + getStripeSize())
                .distinct().limit(amount)
                .toArray();
    }
}
