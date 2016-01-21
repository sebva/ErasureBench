package ch.unine.vauchers.erasuretester.erasure.codes;

import java.util.ArrayList;
import java.util.List;

public class NullErasureCode extends ErasureCode {
    private final int stripeSize;
    private final List<Integer> locationsToRead;

    public NullErasureCode(int stripeSize) {
        this.stripeSize = stripeSize;
        this.locationsToRead = new ArrayList<>(stripeSize);
        for (int i = 0; i < stripeSize; i++) {
            locationsToRead.add(i);
        }
    }

    @Override
    public void encode(int[] message, int[] parity) {

    }

    @Override
    public void decode(int[] data, int[] erasedLocations, int[] erasedValues) {

    }

    @Override
    public void decode(int[] data, int[] erasedLocations, int[] erasedValues, int[] locationsToRead, int[] locationsNotToRead) {

    }

    @Override
    public List<Integer> locationsToReadForDecode(List<Integer> erasedLocations) throws TooManyErasedLocations {
        return locationsToRead;
    }

    @Override
    public int stripeSize() {
        return stripeSize;
    }

    @Override
    public int paritySize() {
        return 0;
    }

    @Override
    public int symbolSize() {
        return 8;
    }
}
