package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.SimpleRegeneratingCode;

public class FileEncoderDecoderSimpleRegeneratingErasureTest extends FileEncoderDecoderTest {
    @Override
    protected FileEncoderDecoder createEncoderDecoder() {
        return new FileEncoderDecoder(new SimpleRegeneratingCode(10, 6, 5), new MemoryStorageBackend());
    }
}
