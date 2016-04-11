package ch.unine.vauchers.erasuretester.erasure.codes;

/**
 *
 */
public class SimpleRegeneratingErasureCodeInstance extends ErasureCodeInstance {

    private final SimpleRegeneratingCode realSut;

    public SimpleRegeneratingErasureCodeInstance() {
        realSut = new SimpleRegeneratingCode(getStripeSize(), getParitySize(), 5);
    }

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
    protected SimpleRegeneratingCode getRealSut() {
        return realSut;
    }
}
