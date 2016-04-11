package ch.unine.vauchers.erasuretester.erasure.codes;

/**
 *
 */
public abstract class ErasureCodeInstance {
    public abstract int getStripeSize();

    public abstract int getParitySize();

    public abstract int getMaxErasures();

    protected abstract ErasureCode getRealSut();
}
