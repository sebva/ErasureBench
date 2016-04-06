package ch.unine.vauchers.erasuretester.erasure.codes;

/**
 *
 */
public class XORErasureCodeTest extends ErasureCodeTest<XORCode> {
    @Override
    public int getStripeSize() {
        return 2;
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
    protected XORCode newSut() {
        return new XORCode(getStripeSize(), getParitySize());
    }
}
