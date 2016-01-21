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

public class FileEncoderDecoder {
    @NotNull
    private final ErasureCode erasureCode;
    @NotNull
    private final StorageBackend storageBackend;
    private final Logger log = Logger.getLogger(FileEncoderDecoder.class.getName());
    private final int totalSize;

    private enum Modes {
        READ_FILE, WRITE_FILE
    }

    public FileEncoderDecoder(@NotNull ErasureCode erasureCode, @NotNull StorageBackend storageBackend) {
        this.erasureCode = erasureCode;
        this.storageBackend = storageBackend;

        assert(erasureCode.symbolSize() == 8);
        totalSize = erasureCode.stripeSize() + erasureCode.paritySize();
    }

    public void readFile(final String path, final int size, final int offset, final ByteBuffer outBuffer) throws TooManyErasedLocations {
        log.info("Reading the file at " + path);

        final FileMetadata metadata = storageBackend.getFileMetadata(path)
                .orElseGet(() -> new FileMetadata().setContentsSize(0).setBlockKeys(Collections.emptyList()));
        final int contentsSize = Math.min(metadata.getContentsSize() - offset, size);
        if (contentsSize <= 0) {
            return;
        }
        final List<String> allBlockKeys = metadata.getBlockKeys().get();

        iterate(size, offset, outBuffer, allBlockKeys, Modes.READ_FILE);
    }

    public void writeFile(String path, int size, int offset, ByteBuffer contents) {
        log.info("Writing the file at " + path);

        final FileMetadata metadata = storageBackend.getFileMetadata(path).orElseGet(FileMetadata::new);
        final int contentsSize = Math.max(Math.min(contents.capacity(), size + offset), metadata.getContentsSize());
        final boolean fileHasGrown = metadata.getContentsSize() < contentsSize;
        metadata.setContentsSize(contentsSize);
        final List<String> blockKeys = metadata.getBlockKeys().orElseGet(() -> new ArrayList<>(contentsSize));
        if (fileHasGrown) {
            while (blockKeys.size() < contentsSize) {
                blockKeys.add(null);
            }
        }

        try {
            iterate(size, offset, contents, blockKeys, Modes.WRITE_FILE);
        } catch (TooManyErasedLocations ignored) {}

        metadata.setBlockKeys(blockKeys);
        storageBackend.setFileMetadata(path, metadata);
    }

    public long sizeOfFile(String path) {
        return storageBackend.getFileMetadata(path).orElse(FileMetadata.EMPTY_METADATA).getContentsSize();
    }

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
            if (i == firstLowerBoundary) {
                offsetPart += blocksToDiscardBeginning;
            }
            int sizePart = erasureCode.stripeSize();
            if (i == lastLowerBoundary) {
                sizePart -= blocksToDiscardEnd;
            }

            ByteBuffer buffer = fileBuffer.slice();
            fileBuffer.position(fileBuffer.position() + sizePart);
            final List<String> keysSublist = blockKeys.subList(i, i + totalSize);
            if (mode == Modes.READ_FILE) {
                readPart(keysSublist, buffer, sizePart, offsetPart);
            } else if (mode == Modes.WRITE_FILE) {
                writePart(keysSublist, buffer, sizePart, offsetPart);
            }
        }
    }

    private void readPart(List<String> blockKeys, ByteBuffer outBuffer, int size, int offset) throws TooManyErasedLocations {
        List<Integer> erasedBlocksIndices = new ArrayList<>();
        Set<String> availableBlocksKeys = new HashSet<>();

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

        final Future<Map<String, Integer>> blocks = storageBackend.retrieveAllBlocksAsync(availableBlocksKeys);

        final Stream<Byte> partData = decodeFileData(blockKeys, blocks, erasedBlocksIndices);

        partData.skip(offset).limit(size).forEach(outBuffer::put);
    }

    private void writePart(List<String> blockKeys, ByteBuffer fileBuffer, int size, int offset) {
        int[] stripeBuffer = new int[erasureCode.stripeSize()];
        int[] parityBuffer = new int[erasureCode.paritySize()];

        final List<Future<Boolean>> blockKeysFutures = new ArrayList<>(totalSize);

        for (int i = 0; i < offset; i++) {
            stripeBuffer[i] = 0;
        }
        for (int i = offset; i < offset + size; i++) {
            stripeBuffer[i] = Byte.toUnsignedInt(fileBuffer.get());
        }

        erasureCode.encode(stripeBuffer, parityBuffer);

        for (int i = 0; i < erasureCode.paritySize(); i++) {
            String key = generateKey();
            blockKeysFutures.add(storageBackend.storeBlockAsync(key, parityBuffer[i]));
            blockKeys.add(i, key);
        }
        for (int i = 0; i < erasureCode.stripeSize(); i++) {
            String key = generateKey();
            blockKeysFutures.add(storageBackend.storeBlockAsync(key, stripeBuffer[i]));
            blockKeys.add(i + erasureCode.paritySize(), key);
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

        final int[] dataBuffer = new int[totalSize];
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
        final double stripeSizeDbl = (double) erasureCode.stripeSize();
        final double paritySizeDbl = (double) erasureCode.paritySize();
        return (int) (mathFunction.apply(index / stripeSizeDbl) * stripeSizeDbl * (1. + paritySizeDbl / stripeSizeDbl));
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
