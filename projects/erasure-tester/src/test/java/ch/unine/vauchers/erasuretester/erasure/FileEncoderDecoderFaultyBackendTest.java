package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;

import java.util.Optional;

public abstract class FileEncoderDecoderFaultyBackendTest extends FileEncoderDecoderTest {
    protected int totalSize;

    @Override
    protected FileEncoderDecoder createEncoderDecoder() {
        final ErasureCode erasureCode = getErasureCode();
        totalSize = erasureCode.stripeSize() + erasureCode.paritySize();
        return new FileEncoderDecoder(erasureCode, new FaultyStorageBackend());
    }

    protected abstract ErasureCode getErasureCode();

    protected abstract boolean isPositionAvailable(int position);

    private class FaultyStorageBackend extends MemoryStorageBackend {
        private int keyThreshold = (int) Math.ceil(StorageBackend.FUSE_READ_SIZE / (double) totalSize);

        private boolean isKeyAvailable(int key) {
            int position = (key / bufferSize) % totalSize;
            return isPositionAvailable(position);
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
    }
}
