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
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Intermediate layer between the frontend, the storage backend and erasure coding.
 */
public class FileEncoderDecoder {
    @NotNull
    private final ErasureCode erasureCode;
    @NotNull
    private final StorageBackend storageBackend;
    private final Logger log = Logger.getLogger(FileEncoderDecoder.class.getName());
    private final int totalSize;

    // Field used in decode/encode methods, declared globally for better performance
    private final List<Integer> erasedBlocksIndices;
    private final Set<String> availableBlocksKeys;
    private final int[] stripeBuffer;
    private final int[] parityBuffer;
    private final List<Future<Boolean>> blockKeysFutures;
    private final int[] dataBuffer;

    private enum Modes {
        READ_FILE, WRITE_FILE
    }

    /**
     * Constructor
     * @param erasureCode The erasure coding implementation to use
     * @param storageBackend The storage backend implementation to use
     */
    public FileEncoderDecoder(@NotNull ErasureCode erasureCode, @NotNull StorageBackend storageBackend) {
        this.erasureCode = erasureCode;
        this.storageBackend = storageBackend;

        // Only works with bytes
        assert(erasureCode.symbolSize() == 8);
        totalSize = erasureCode.stripeSize() + erasureCode.paritySize();

        erasedBlocksIndices = new ArrayList<>();
        availableBlocksKeys = new HashSet<>();
        stripeBuffer = new int[erasureCode.stripeSize()];
        parityBuffer = new int[erasureCode.paritySize()];
        blockKeysFutures = new ArrayList<>(totalSize);
        dataBuffer = new int[totalSize];
    }

    /**
     * Read a previously stored file from storage, and decode it
     * @param path String uniquely identifying a file
     * @param size How much bytes of contents to read
     * @param offset At which byte index the reading has to start
     * @param outBuffer The contents of the file will be written to this buffer. It needs to be previously allocated
     *                  with enough space to fit the size passed in parameter. Writing will start wherever the buffer
     *                  is positioned.
     * @throws TooManyErasedLocations Due to too many unavailable blocks, the contents can not be retrieved.
     */
    public void readFile(final String path, final int size, final int offset, @NotNull final ByteBuffer outBuffer) throws TooManyErasedLocations {
        log.info("Reading the file at " + path);

        final FileMetadata metadata = storageBackend.getFileMetadata(path)
                .orElseGet(() -> new FileMetadata().setContentsSize(0).setBlockKeys(Collections.emptyList()));
        final int contentsSize = Math.min(metadata.getContentsSize() - offset, size);
        if (contentsSize <= 0) {
            return;
        }
        final List<String> allBlockKeys = metadata.getBlockKeys().get();

        iterate(contentsSize, offset, outBuffer, allBlockKeys, Modes.READ_FILE);
    }

    /**
     * Write the contents of a file to storage, with erasure coding applied
     * @param path String uniquely identifying the file
     * @param size How much bytes of contents to write
     * @param offset At which byte index do the writing starts (relative to the complete file)
     * @param contents The data to write into the file. The buffer will be read starting at its current position.
     */
    public void writeFile(String path, int size, int offset, @NotNull ByteBuffer contents) {
        log.info("Writing the file at " + path);

        final FileMetadata metadata = storageBackend.getFileMetadata(path).orElseGet(FileMetadata::new);
        final int iterationSize = Math.min(contents.limit(), size);
        final int contentsSize = Math.max(iterationSize + offset, metadata.getContentsSize());
        metadata.setContentsSize(contentsSize);
        final List<String> blockKeys = metadata.getBlockKeys().orElseGet(() -> new ArrayList<>(contentsSize));

        final int nextBoundary = nextBoundary(contentsSize);
        while (blockKeys.size() < nextBoundary) {
            // Grow the blockKeys list to fit the size/offset given in parameter
            blockKeys.add(null);
        }

        try {
            iterate(iterationSize, offset, contents, blockKeys, Modes.WRITE_FILE);
        } catch (TooManyErasedLocations ignored) {} // Will never happen

        metadata.setBlockKeys(blockKeys);
        storageBackend.setFileMetadata(path, metadata);
    }

    /**
     * Return the size of a given file
     * @param path String uniquely identifying a file
     * @return The size of the contents of the file, in bytes
     */
    public long sizeOfFile(String path) {
        return storageBackend.getFileMetadata(path).orElse(FileMetadata.EMPTY_METADATA).getContentsSize();
    }

    /**
     * Truncate the size of a given file to a given size
     * @param filepath String uniquely identifying a file
     * @param size The new size of the file
     */
    public void truncate(final String filepath, final int size) {
        storageBackend.getFileMetadata(filepath).ifPresent(metadata -> {
            final int newSize = Math.min(metadata.getContentsSize(), size);
            metadata.setContentsSize(newSize);
            metadata.setBlockKeys(metadata.getBlockKeys().orElseThrow(() -> new RuntimeException("Block keys list is null"))
                    .subList(0, newSize));
        });
    }

    private void iterate(int size, int offset, ByteBuffer fileBuffer, List<String> blockKeys, Modes mode) throws TooManyErasedLocations {
        final int firstLowerBoundary = previousBoundary(offset);
        final int lastLowerBoundary = previousBoundary(offset + size);
        final int blocksToDiscardBeginning = lowerBytesToDrop(offset);
        final int blocksToDiscardEnd = higherBytesToDrop(offset + size);

        for (int i = firstLowerBoundary; i <= lastLowerBoundary; i += totalSize) {
            int offsetPart = 0;
            int sizePart = erasureCode.stripeSize();
            if (i == firstLowerBoundary) {
                offsetPart += blocksToDiscardBeginning;
                sizePart -= blocksToDiscardBeginning;
            } else if (i == lastLowerBoundary) {
                if (blocksToDiscardEnd == 0) {
                    break;
                } else {
                    sizePart -= blocksToDiscardEnd;
                }
            }

            ByteBuffer buffer = fileBuffer.slice();
            if (i < lastLowerBoundary) {
                fileBuffer.position(fileBuffer.position() + sizePart);
            }
            final List<String> keysSublist = blockKeys.subList(i, i + totalSize);
            if (mode == Modes.READ_FILE) {
                readPart(keysSublist, buffer, sizePart, offsetPart);
            } else if (mode == Modes.WRITE_FILE) {
                writePart(keysSublist, buffer, sizePart, offsetPart);
            }
        }
    }

    private void readPart(List<String> blockKeys, ByteBuffer outBuffer, int size, int offset) throws TooManyErasedLocations {
        availableBlocksKeys.clear();
        erasedBlocksIndices.clear();

        final Iterator<Future<Boolean>> blocksAvailableIterator = blockKeys.stream().map(storageBackend::isBlockAvailableAsync).iterator();
        for (int i = 0; i < totalSize; i++) {
            try {
                if (blocksAvailableIterator.next().get()) {
                    availableBlocksKeys.add(blockKeys.get(i));
                } else {
                    erasedBlocksIndices.add(i);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                erasedBlocksIndices.add(i);
            }
        }

        /* We can end up loading more blocks than necessary
           TODO Only load blocks that are really needed */
        final Future<Map<String, Integer>> blocks = storageBackend.retrieveAllBlocksAsync(availableBlocksKeys);

        final Stream<Byte> partData = decodeFileData(blockKeys, blocks, erasedBlocksIndices);

        partData.skip(offset).limit(size).forEach(outBuffer::put);
    }

    private void writePart(List<String> blockKeys, ByteBuffer fileBuffer, int size, int offset) {
        blockKeysFutures.clear();

        for (int i = 0; i < erasureCode.stripeSize(); i++) {
            if (i < offset || i >= offset + size) { // Restore existing data
                final String key = blockKeys.get(i + erasureCode.paritySize());
                if (key != null) {
                    stripeBuffer[i] = storageBackend.retrieveBlock(key).orElse(0);
                } else {
                    stripeBuffer[i] = 0;
                }
            } else {
                stripeBuffer[i] = Byte.toUnsignedInt(fileBuffer.get());
            }
        }

        erasureCode.encode(stripeBuffer, parityBuffer);

        for (int i = 0; i < erasureCode.paritySize(); i++) {
            String key = generateKey();
            blockKeysFutures.add(storageBackend.storeBlockAsync(key, parityBuffer[i]));
            blockKeys.set(i, key);
        }
        for (int i = 0; i < erasureCode.stripeSize(); i++) {
            String key = generateKey();
            blockKeysFutures.add(storageBackend.storeBlockAsync(key, stripeBuffer[i]));
            blockKeys.set(i + erasureCode.paritySize(), key);
        }

        blockKeysFutures.forEach((booleanFuture) -> {
            try {
                booleanFuture.get(2, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        });
    }

    private String generateKey() {
        return UUID.randomUUID().toString();
    }

    private Stream<Byte> decodeFileData(List<String> blockKeys, Future<Map<String, Integer>> blocksFuture, List<Integer> erasedIndices) throws TooManyErasedLocations {
        final List<Integer> toReadForDecode = erasureCode.locationsToReadForDecode(erasedIndices);
        toReadForDecode.sort(null);

        toReadForDecode.stream().forEach(index -> {
            final String key = blockKeys.get(index);
            try {
                dataBuffer[index] = blocksFuture.get().get(key);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        final int[] recoveredValues = new int[erasedIndices.size()];
        erasureCode.decode(dataBuffer, convertToIntArray(erasedIndices), recoveredValues, convertToIntArray(toReadForDecode), fillNotToRead(toReadForDecode));

        // Restore erased values
        final PrimitiveIterator.OfInt recoveredValuesIterator = Arrays.stream(recoveredValues).iterator();
        erasedIndices.forEach(erasedIndex -> dataBuffer[erasedIndex] = recoveredValues[recoveredValuesIterator.next()]);

        return Arrays.stream(dataBuffer).skip(erasureCode.paritySize()).boxed().map(Integer::byteValue);
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

    int nextBoundary(int index) {
        return computeBoundary(Math::ceil, index);
    }

    int previousBoundary(int index) {
        return computeBoundary(Math::floor, index);
    }

    private int computeBoundary(Function<Double, Double> mathFunction, int index) {
        return (int) Math.round(mathFunction.apply(index / (double) erasureCode.stripeSize()) * totalSize);
    }

    int lowerBytesToDrop(int index) {
        return index % erasureCode.stripeSize();
    }

    int higherBytesToDrop(int index) {
        final int lowerBytesToDrop = lowerBytesToDrop(index);
        if (index == 0) {
            return erasureCode.stripeSize();
        } else if (lowerBytesToDrop == 0) {
            return 0;
        }
        return erasureCode.stripeSize() - lowerBytesToDrop;
    }
}
