package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.SimpleRegeneratingCode;

import java.util.Collections;

public class FileEncoderDecoderSimpleRegeneratingErasureTest extends FileEncoderDecoderTest {
    @Override
    protected Iterable<FileEncoderDecoder> createEncoderDecoder() {
        return Collections.singleton(new SimpleRegeneratingFileEncoderDecoder(new SimpleRegeneratingCode(10, 6, 5), new MemoryStorageBackend()));
    }
}
