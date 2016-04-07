package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.XORCode;

public class FileEncoderDecoderFaultyBackendXorTest extends FileEncoderDecoderFaultyBackendTest {

    @Override
    protected ErasureCode getErasureCode() {
        return new XORCode(2, 1);
    }

    @Override
    protected int getMaxFaults() {
        return 1;
    }

}
