package ch.unine.vauchers.erasuretester.erasure.codes;

/**
 *
 */
public class ReedSolomonErasureCodeInstance extends ErasureCodeInstance {
    private final ReedSolomonCode realSut;

    public ReedSolomonErasureCodeInstance() {
        this.realSut = new ReedSolomonCode(getStripeSize(), getParitySize());
    }

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
    protected ReedSolomonCode getRealSut() {
        return realSut;
    }
}
