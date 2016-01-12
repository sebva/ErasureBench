package ch.unine.vauchers.fuseerasure;

import ch.unine.vauchers.fuseerasure.codes.ErasureCode;

import java.nio.ByteBuffer;

public class EncDec {
    private final ErasureCode erasureCode;
    private int contentsSize = 0;
    /**
     * Concatenation of parity bits + data bits
     */
    private ByteBuffer data;
    private final int[] stripeBuffer;
    private final int[] parityBuffer;
    private final int[] stripeParityBuffer;
    private final ErasureGenerator erasureGenerator;

    public EncDec(ErasureCode erasureCode) {
        this(erasureCode, (paritySize, stripeSize) -> new int[0]);
    }

    public interface ErasureGenerator {
        int[] getErasedLocations(int paritySize, int stripeSize);
    }

    public EncDec(ErasureCode erasureCode, ErasureGenerator erasureGenerator) {
        this.erasureCode = erasureCode;
        this.erasureGenerator = erasureGenerator;
        stripeBuffer = new int[erasureCode.stripeSize()];
        parityBuffer = new int[erasureCode.paritySize()];
        stripeParityBuffer = new int[erasureCode.stripeSize() + erasureCode.paritySize()];
    }

    public long size() {
        return contentsSize;
    }

    public ByteBuffer restoreContents() {
        if (data == null) {
            return ByteBuffer.allocate(0);
        }

        ByteBuffer contents = ByteBuffer.allocate(contentsSize);
        data.rewind();
        while (data.hasRemaining()) {
            int totalSize = erasureCode.stripeSize() + erasureCode.paritySize();
            for (int i = 0; i < totalSize; i++) {
                stripeParityBuffer[i] = data.get();
            }

            int[] erasedLocations = erasureGenerator.getErasedLocations(erasureCode.paritySize(), erasureCode.stripeSize());
            int[] recoveredValues = new int[erasedLocations.length];
            erasureCode.decode(stripeParityBuffer, erasedLocations, recoveredValues);
            for (int i = 0; i < erasedLocations.length; i++) {
                stripeParityBuffer[erasedLocations[i]] = recoveredValues[i];
            }

            for (int i = erasureCode.paritySize(); i < totalSize; i++) {
                if (contents.hasRemaining()) {
                    contents.put((byte) stripeParityBuffer[i]);
                }
            }
        }

        return contents;
    }

    public void storeContents(ByteBuffer contents) {
        contents.rewind();

        contentsSize = contents.capacity();
        int dataSize = computeDataSize(contentsSize);
        allocate(dataSize);
        data.rewind();

        while (contents.hasRemaining()) {
            int length = Math.min(contents.remaining(), erasureCode.stripeSize());
            for (int i = 0; i < length; i++) {
                stripeBuffer[i] = contents.get();
            }

            erasureCode.encode(stripeBuffer, parityBuffer);
            for (int value : parityBuffer) {
                data.put((byte) value);
            }
            for (int value : stripeBuffer) {
                data.put((byte) value);
            }
        }
    }

    private void allocate(int dataSize) {
        if (data == null || data.capacity() < dataSize) {
            data = ByteBuffer.allocate(dataSize);
        } else {
            data.limit(dataSize);
        }
    }

    int computeDataSize(int contentsSize) {
        double dataSize = Math.ceil(contentsSize / (double) erasureCode.stripeSize()) * erasureCode.stripeSize();
        dataSize *= 1. + erasureCode.paritySize() / (double) erasureCode.stripeSize();
        return (int) Math.round(dataSize);
    }
}
