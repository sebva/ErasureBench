package ch.unine.vauchers.erasuretester;

import ch.unine.vauchers.erasuretester.backend.RedisStorageBackend;
import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.FileEncoderDecoder;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.ReedSolomonCode;
import ch.unine.vauchers.erasuretester.frontend.FuseMemoryFrontend;
import ru.serce.jnrfuse.FuseException;

import java.nio.file.FileSystems;

/**
 * Entry-point of the application
 */
public class Main {

    public static void main(String[] argv) {
        // Disable logging completely for faster performance
        // Utils.disableLogging();

        ErasureCode erasureCode = new ReedSolomonCode(10, 4);
        StorageBackend storageBackend = new RedisStorageBackend();
        FileEncoderDecoder encdec = new FileEncoderDecoder(erasureCode, storageBackend);

        final FuseMemoryFrontend fuse = new FuseMemoryFrontend(encdec, false);

        // Gracefully quit on Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    storageBackend.disconnect();
                    fuse.umount();
                } catch (FuseException e) {
                    e.printStackTrace();
                }
            }
        });

        fuse.mount(FileSystems.getDefault().getPath(argv[0]));
    }

}
