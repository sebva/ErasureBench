package ch.unine.vauchers.erasuretester.erasure.codes;

/**
 *
 */
public class XORErasureCodeInstance extends ErasureCodeInstance {

    private final XORCode realSut;

    public XORErasureCodeInstance() {
        realSut = new XORCode(getStripeSize(), getParitySize());
    }

    @Override
    public int getStripeSize() {
        return 4;
    }

    @Override
    public int getParitySize() {
        return 1;
    }

    @Override
    public int getMaxErasures() {
        return getParitySize();
    }

    @Override
    protected XORCode getRealSut() {
        return realSut;
    }
}
