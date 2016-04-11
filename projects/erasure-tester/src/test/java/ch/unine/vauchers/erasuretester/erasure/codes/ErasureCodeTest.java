package ch.unine.vauchers.erasuretester.erasure.codes;

import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 *
 */
@RunWith(Parameterized.class)
public class ErasureCodeTest {
    private final int numberOfErasures;
    private ErasureCodeInstance sutWrapper;
    private ErasureCode sut;
    private static Random random = new Random(945709326542L);

    // data and data2 contain the same values. Use one for input and one for checking.
    private int[] data;
    private int[] data2;
    // Pre-instantiated output arrays
    private int[] stripe;
    private int[] parity;

    public ErasureCodeTest(ErasureCodeInstance sutWrapper, int numberOfErasures) {
        this.sutWrapper = sutWrapper;
        this.sut = sutWrapper.newSut();
        this.numberOfErasures = numberOfErasures;
    }

    @Parameterized.Parameters(name = "{0}, {1} erasures")
    public static Collection<Object[]> parameters() {
        return Arrays.stream(new ErasureCodeInstance[]{
                new XORErasureCodeInstance(),
                new ReedSolomonErasureCodeInstance(),
                new SimpleRegeneratingErasureCodeInstance()
        }).flatMap(erasureCodeInstance ->
                IntStream.rangeClosed(0, erasureCodeInstance.getStripeSize() + erasureCodeInstance.getParitySize())
                        .boxed()
                        .map(value -> new Object[]{erasureCodeInstance, value})
        ).collect(Collectors.toList());
    }

    @Before
    public void setup() {
        data = new int[sutWrapper.getStripeSize()];
        data2 = new int[sutWrapper.getStripeSize()];
        byte[] bytesData = new byte[sutWrapper.getStripeSize()];
        random.nextBytes(bytesData);
        for (int i = 0; i < sutWrapper.getStripeSize(); i++) {
            data[i] = data2[i] = Byte.toUnsignedInt(bytesData[i]);
        }
        parity = new int[sutWrapper.getParitySize()];
    }

    @Test
    public void testSymbolSize() {
        Assert.assertEquals(8, sut.symbolSize());
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
        for (int iteration = 0; iteration < 10000; iteration++) {
            System.arraycopy(data2, 0, data, 0, data.length);
            checkData();

            int[] erasures = generateErasures(numberOfErasures);

            int[] mergedParityData = mergeArrays(parity, data);
            for (int erasure : erasures) {
                mergedParityData[erasure] = 0;
                if (erasure >= sutWrapper.getParitySize()) {
                    data[erasure - sutWrapper.getParitySize()] = 0;
                }
            }

            final IntList locationsToReadForDecode;
            try {
                sut.locationsToReadForDecode(Arrays.stream(erasures).boxed().collect(Collectors.toList()));
                locationsToReadForDecode = sut.locationsToReadForDecode(Arrays.stream(erasures).boxed().collect(Collectors.toList()));
                locationsToReadForDecode.sort(null);
            } catch (TooManyErasedLocations e) {
                if (erasures.length <= sutWrapper.getMaxErasures() && !(sutWrapper instanceof SimpleRegeneratingErasureCodeInstance)) {
                    Assert.fail();
                    return;
                } else {
                    continue;
                }
            }
            final int[] locationsNotToRead = fillNotToRead(locationsToReadForDecode);
            for (int ntr : locationsNotToRead) {
                mergedParityData[ntr] = 0;
            }

            int[] recoveredValues = new int[erasures.length];

            sut.decode(mergedParityData, erasures, recoveredValues, locationsToReadForDecode.toIntArray(), locationsNotToRead);
            restoreValues(data, erasures, recoveredValues);
            checkData(numberOfErasures);
        }
    }

    private int[] fillNotToRead(IntList toReadForDecode) {
        final int totalSize = sutWrapper.getStripeSize() + sutWrapper.getParitySize();

        final int trSize = toReadForDecode.size();
        int[] notToRead = new int[totalSize - trSize];

        int ntrIndex = 0;
        int trIndex = 0;
        for (int globIndex = 0; globIndex < totalSize; globIndex++) {
            if (trIndex < trSize && toReadForDecode.getInt(trIndex) == globIndex) {
                trIndex++;
            } else {
                notToRead[ntrIndex++] = globIndex;
            }
        }

        return notToRead;
    }

    private int[] mergeArrays(int[] parity, int[] data) {
        return IntStream.concat(Arrays.stream(parity), Arrays.stream(data)).toArray();
    }

    private void restoreValues(int[] data, int[] recoveredIndices, int[] recoveredValues) {
        final int paritySize = sutWrapper.getParitySize();
        for (int i = 0; i < recoveredIndices.length; i++) {
            if (recoveredIndices[i] >= paritySize) {
                data[recoveredIndices[i] - paritySize] = recoveredValues[i];
            }
        }
    }

    private void checkData(int i) {
        checkData();
        System.out.println(i + " erasures OK");
    }

    private void checkData() {
        Assert.assertEquals(Arrays.toString(data2), Arrays.toString(data));
    }

    private int[] generateErasures(int amount) {
        final int[] erasures = random.ints(250, 0, sutWrapper.getParitySize() + sutWrapper.getStripeSize())
                .distinct().limit(amount)
                .toArray();
        Assert.assertEquals(amount, erasures.length);
        return erasures;
    }
}
