package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.ReedSolomonCode;

public class FileEncoderDecoderFaultyBackendReedSolomonTest extends FileEncoderDecoderFaultyBackendTest {

    @Override
    protected ErasureCode getErasureCode() {
        return new ReedSolomonCode(10, 4);
    }

    @Override
    protected boolean isPositionAvailable(int position) {
        return position >= 2 && position <= 11;
    }
}
