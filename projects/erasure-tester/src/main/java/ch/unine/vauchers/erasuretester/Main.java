package ch.unine.vauchers.erasuretester;

import ch.unine.vauchers.erasuretester.backend.JedisStorageBackend;
import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.FileEncoderDecoder;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.NullErasureCode;
import ch.unine.vauchers.erasuretester.frontend.FuseMemoryFrontend;
import net.fusejna.FuseException;

import java.io.IOException;

/**
 * Entry-point of the application
 */
public class Main {

    public static void main(String[] argv) throws FuseException {
        // Disable logging completely for faster performance
        Utils.disableLogging();

        ErasureCode erasureCode = new NullErasureCode(10);
        StorageBackend storageBackend = new JedisStorageBackend();
        FileEncoderDecoder encdec = new FileEncoderDecoder(erasureCode, storageBackend);

        final FuseMemoryFrontend fuse = new FuseMemoryFrontend(encdec, false);
        // Gracefully quit on Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    storageBackend.disconnect();
                    fuse.unmount();
                } catch (IOException | FuseException e) {
                    e.printStackTrace();
                }
            }
        });

        fuse.mount(argv[0]);
    }

}
