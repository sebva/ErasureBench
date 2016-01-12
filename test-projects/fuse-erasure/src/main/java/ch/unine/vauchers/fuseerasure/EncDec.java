package ch.unine.vauchers.fuseerasure;

import ch.unine.vauchers.fuseerasure.codes.ErasureCode;

import java.nio.ByteBuffer;

public class EncDec {
    private final ErasureCode erasureCode;
    private byte[] storage;

    public EncDec() {
        erasureCode = ErasureCodeFactory.getErasureCode();
    }

    public long size() {
        if (storage == null) {
            return 0;
        }
        return storage.length;
    }

    public ByteBuffer restoreContents() {
        if (storage == null) {
            return ByteBuffer.allocate(0);
        }
        return ByteBuffer.wrap(storage);
    }


    public void storeContents(ByteBuffer contents) {
        storage = contents.array();
    }
}
