package ch.unine.vauchers.erasuretester.erasure.codes;

/**
 *
 */
public class ReedSolomonErasureCodeTest extends ErasureCodeTest<ReedSolomonCode> {
    @Override
    public int getStripeSize() {
        return 10;
    }

    @Override
    public int getParitySize() {
        return 4;
    }

    @Override
    public int getMaxErasures() {
        return getParitySize();
    }

    @Override
    protected ReedSolomonCode newSut() {
        return new ReedSolomonCode(getStripeSize(), getParitySize());
    }
}
