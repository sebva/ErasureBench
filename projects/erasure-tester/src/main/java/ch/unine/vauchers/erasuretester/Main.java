package ch.unine.vauchers.erasuretester;

import ch.unine.vauchers.erasuretester.backend.FailureGenerator;
import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.backend.NullFailureGenerator;
import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.FileEncoderDecoder;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.ReedSolomonCode;
import ch.unine.vauchers.erasuretester.frontend.FuseMemoryFrontend;
import net.fusejna.FuseException;

import java.util.logging.LogManager;

public class Main {

    public static void main(String[] argv) throws FuseException {
        // Disable logging completely for faster performance
        // LogManager.getLogManager().reset();

        ErasureCode erasureCode = new ReedSolomonCode(10, 4);
        FailureGenerator failureGenerator = new NullFailureGenerator();
        StorageBackend storageBackend = new MemoryStorageBackend(failureGenerator);
        FileEncoderDecoder encdec = new FileEncoderDecoder(erasureCode, storageBackend);
        new FuseMemoryFrontend(encdec, false).mount(argv[0]);
    }

}
