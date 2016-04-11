package ch.unine.vauchers.erasuretester.erasure.codes;

/**
 *
 */
public class ReedSolomonErasureCodeInstance extends ErasureCodeInstance {

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

    @Override
    public String toString() {
        return "ReedSolomon";
    }
}
