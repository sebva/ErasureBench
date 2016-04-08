package ch.unine.vauchers.erasuretester.erasure.codes;

/**
 *
 */
public class SimpleRegeneratingErasureCodeTest extends ErasureCodeTest<SimpleRegeneratingCode> {
    @Override
    public int getStripeSize() {
        return 10;
    }

    @Override
    public int getParitySize() {
        return 6;
    }

    @Override
    public int getMaxErasures() {
        return 4;
    }

    @Override
    protected SimpleRegeneratingCode newSut() {
        return new SimpleRegeneratingCode(getStripeSize(), getParitySize(), 2);
    }
}
