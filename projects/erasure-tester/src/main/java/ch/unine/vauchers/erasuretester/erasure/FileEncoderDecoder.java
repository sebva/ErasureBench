package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.FileMetadata;
import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.TooManyErasedLocations;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class FileEncoderDecoder {
    @NotNull
    private final ErasureCode erasureCode;
    @NotNull
    private final StorageBackend storageBackend;
    private final Logger log = Logger.getLogger(FileEncoderDecoder.class.getName());
    private final int totalSize;

    public FileEncoderDecoder(@NotNull ErasureCode erasureCode, @NotNull StorageBackend storageBackend) {
        this.erasureCode = erasureCode;
        this.storageBackend = storageBackend;

        assert(erasureCode.symbolSize() == 8);
        totalSize = erasureCode.stripeSize() + erasureCode.paritySize();
    }

    public ByteBuffer readFile(String path) throws TooManyErasedLocations {
        Optional<FileMetadata> metadataOptional = storageBackend.getFileMetadata(path);
        if (!metadataOptional.isPresent()) {
            return ByteBuffer.allocate(0);
        }
        FileMetadata metadata = metadataOptional.get();
        int contentsSize = metadata.getContentsSize();
        List<String> blockKeys = metadata.getBlockKeys();

        log.info("Reading the file at " + path);
        final ByteBuffer fileDataBuffer = ByteBuffer.allocate(contentsSize);
        List<Integer> erasedIndices = new ArrayList<>(totalSize);

        final int[] dataBuffer = new int[totalSize];

        final ListIterator<String> blocksIterator = blockKeys.listIterator();
        int globalBlockIndex = 0;

        while (fileDataBuffer.position() < contentsSize) {
            erasedIndices.clear();

            for (int i = 0; i < totalSize; i++) {
                String key = blocksIterator.next();
                if (!storageBackend.isBlockAvailable(key)) {
                    erasedIndices.add(i);
                }
            }

            final List<Integer> toReadForDecode = erasureCode.locationsToReadForDecode(erasedIndices);
            toReadForDecode.sort(null);
            for (int index : toReadForDecode) {
                final String key = blockKeys.get(index + globalBlockIndex);
                dataBuffer[index] = storageBackend.retrieveBlock(key).orElseThrow(
                        () -> new RuntimeException("Storage backend unable to retrieve block marked as available"));
            }

            final int[] recoveredValues = new int[erasedIndices.size()];
            erasureCode.decode(dataBuffer, convertToIntArray(erasedIndices), recoveredValues, convertToIntArray(toReadForDecode), fillNotToRead(toReadForDecode));

            int recoveredValuesIndex = 0;
            for (int erasedIndex : erasedIndices) {
                dataBuffer[erasedIndex] = recoveredValues[recoveredValuesIndex++];
            }

            for (int i = erasureCode.paritySize(); i < totalSize; i++) {
                if (fileDataBuffer.position() < contentsSize) {
                    fileDataBuffer.put((byte) dataBuffer[i]);
                }
            }

            globalBlockIndex += totalSize;
        }

        return fileDataBuffer;
    }

    private int[] fillNotToRead(List<Integer> toReadForDecode) {
        int[] notToRead = new int[totalSize - toReadForDecode.size()];

        int previousIndex = -1;
        int notToReadIndex = 0;
        for (int toReadIndex : toReadForDecode) {
            for (int i = previousIndex + 1; i < toReadIndex; i++) {
                notToRead[notToReadIndex++] = i;
            }
            previousIndex = toReadIndex;
        }

        return notToRead;
    }

    private static int[] convertToIntArray(Collection<Integer> integerCollection) {
        return integerCollection.stream().mapToInt(i->i).toArray();
    }

    public void writeFile(String path, ByteBuffer contents) {
        contents.rewind();
        FileMetadata metadata = new FileMetadata();
        final int contentsSize = contents.capacity();
        metadata.setContentsSize(contentsSize);

        int[] stripeBuffer = new int[erasureCode.stripeSize()];
        int[] parityBuffer = new int[erasureCode.paritySize()];

        final int numberOfBlocks = computeDataSize(contentsSize);
        final List<Future<Boolean>> blockKeysFutures = new ArrayList<>(numberOfBlocks);
        final List<String> blockKeys = new ArrayList<>(numberOfBlocks);

        log.info("Writing the file at " + path);
        int blockCounter = 0;
        while (contents.hasRemaining()) {
            int bytesToRead = Math.min(contents.remaining(), erasureCode.stripeSize());
            for (int i = 0; i < bytesToRead; i++) {
                stripeBuffer[i] = Byte.toUnsignedInt(contents.get());
            }

            erasureCode.encode(stripeBuffer, parityBuffer);

            for (int block : parityBuffer) {
                String key = path + blockCounter++;
                blockKeysFutures.add(storageBackend.storeBlockAsync(key, block));
                blockKeys.add(key);
            }
            for (int block : stripeBuffer) {
                String key = path + blockCounter++;
                blockKeysFutures.add(storageBackend.storeBlockAsync(key, block));
                blockKeys.add(key);
            }
        }

        blockKeysFutures.forEach((booleanFuture) -> {
            try {
                booleanFuture.get(2, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        });
        metadata.setBlockKeys(blockKeys);
        storageBackend.setFileMetadata(path, metadata);
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
