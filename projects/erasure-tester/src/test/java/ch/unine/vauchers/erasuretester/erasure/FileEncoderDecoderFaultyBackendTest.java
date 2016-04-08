package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class FileEncoderDecoderFaultyBackendTest extends FileEncoderDecoderTest {
    protected int totalSize;

    @Override
    protected Iterable<FileEncoderDecoder> createEncoderDecoder() {
        final ErasureCode erasureCode = getErasureCode();
        totalSize = erasureCode.stripeSize() + erasureCode.paritySize();
        return IntStream
                .rangeClosed(0, getMaxFaults())
                .boxed()
                .flatMap(this::instantiateFaultyBackends)
                .map(faultyStorageBackend -> new FileEncoderDecoder(getErasureCode(), faultyStorageBackend))
                .collect(Collectors.toList());
    }

    protected abstract ErasureCode getErasureCode();

    protected abstract int getMaxFaults();

    private Stream<FaultyStorageBackend> instantiateFaultyBackends(int numberOfFailures) {
        return IntStream.range(0, 10)
                .boxed()
                .map(integer -> {
                    List<Integer> faultyPositions = IntStream.range(0, totalSize).boxed().collect(Collectors.toList());
                    Collections.shuffle(faultyPositions);
                    final Set<Integer> faultyPositionsSet = faultyPositions.stream().limit(numberOfFailures).collect(Collectors.toSet());
                    return new FaultyStorageBackend(faultyPositionsSet);
                });
    }

    private class FaultyStorageBackend extends MemoryStorageBackend {
        private final Set<Integer> faultyPositions;

        public FaultyStorageBackend(Set<Integer> faultyPositions) {
            this.faultyPositions = faultyPositions;
        }

        private boolean isKeyAvailable(int key) {
            int position = Math.floorMod(key / bufferSize, totalSize);
            return !faultyPositions.contains(position);
        }

        @Override
        public boolean isBlockAvailable(int key) {
            return isKeyAvailable(key) && super.isBlockAvailable(key);
        }

        @Override
        public Optional<Integer> retrieveBlock(int key) {
            if (isKeyAvailable(key)) {
                return super.retrieveBlock(key);
            } else {
                return Optional.empty();
            }
        }

        @Override
        public String toString() {
            return "FaultyStorageBackend{" +
                    "faultyPositions=" + faultyPositions +
                    '}';
        }
    }
}
