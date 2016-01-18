package ch.unine.vauchers.erasuretester;

import ch.unine.vauchers.erasuretester.backend.FailureGenerator;
import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.backend.NullFailureGenerator;
import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.FileEncoderDecoder;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.ReedSolomonCode;
import ch.unine.vauchers.erasuretester.erasure.codes.SimpleRegeneratingCode;
import ch.unine.vauchers.erasuretester.erasure.codes.XORCode;
import ch.unine.vauchers.erasuretester.frontend.FuseMemoryFrontend;
import net.fusejna.FuseException;

public class Main {

    public static void main(String[] argv) throws FuseException {
        ErasureCode erasureCode = new ReedSolomonCode(10, 4);
        FailureGenerator failureGenerator = new NullFailureGenerator();
        StorageBackend storageBackend = new MemoryStorageBackend(failureGenerator);
        FileEncoderDecoder encdec = new FileEncoderDecoder(erasureCode, storageBackend);
        new FuseMemoryFrontend(encdec, false).mount(argv[0]);
    }

}
