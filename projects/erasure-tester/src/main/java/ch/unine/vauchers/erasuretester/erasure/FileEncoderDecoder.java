package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.FileMetadata;
import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class FileEncoderDecoder {
    @NotNull
    private final ErasureCode erasureCode;
    @NotNull
    private final StorageBackend storageBackend;
    private final Logger log = Logger.getLogger(FileEncoderDecoder.class.getName());

    public FileEncoderDecoder(@NotNull ErasureCode erasureCode, @NotNull StorageBackend storageBackend) {
        this.erasureCode = erasureCode;
        this.storageBackend = storageBackend;
    }

    public ByteBuffer readFile(String path) {
        Optional<FileMetadata> metadataOptional = storageBackend.getFileMetadata(path);
        if (!metadataOptional.isPresent()) {
            return ByteBuffer.allocate(0);
        }
        FileMetadata metadata = metadataOptional.get();
        int contentsSize = metadata.getContentsSize();
        List blockKeys = metadata.getBlockKeys();

        log.info("Reading the file at " + path);
        // TODO Read the file
        return ByteBuffer.allocate(0);
    }

    public void writeFile(String path, ByteBuffer contents) {
        contents.rewind();
        FileMetadata metadata = new FileMetadata();
        final int contentsSize = contents.capacity();
        metadata.setContentsSize(contentsSize);
        List blockKeys = new ArrayList<>(computeDataSize(contentsSize));

        log.info("Writing the file at " + path);
        // TODO Save the file
    }

    int computeDataSize(int contentsSize) {
        double dataSize = Math.ceil(contentsSize / (double) erasureCode.stripeSize()) * erasureCode.stripeSize();
        dataSize *= 1. + erasureCode.paritySize() / (double) erasureCode.stripeSize();
        return (int) Math.round(dataSize);
    }

    public long sizeOfFile(String path) {
        return storageBackend.getFileMetadata(path).orElse(FileMetadata.EMPTY_METADATA).getContentsSize();
    }
}
