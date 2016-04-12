package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.SimpleRegeneratingCode;

public class FileEncoderDecoderFaultyBackendSimpleRegeneratingTest extends FileEncoderDecoderFaultyBackendTest {

    @Override
    protected ErasureCode getErasureCode() {
        return new SimpleRegeneratingCode(10, 6, 5);
    }

    @Override
    protected int getMaxFaults() {
        return 4;
    }
}
