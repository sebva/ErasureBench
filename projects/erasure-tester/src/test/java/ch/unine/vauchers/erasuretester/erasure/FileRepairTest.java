package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.ReedSolomonCode;
import ch.unine.vauchers.erasuretester.erasure.codes.SimpleRegeneratingCode;
import ch.unine.vauchers.erasuretester.erasure.codes.TooManyErasedLocations;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 *
 */
@RunWith(Parameterized.class)
public class FileRepairTest {
    private FileEncoderDecoder fed;
    private Mode mode;
    private final int stripeSize;
    private final int paritySize;

    public FileRepairTest(ErasureCode code) {
        this.mode = Mode.FAULTY;
        this.stripeSize = code.stripeSize();
        this.paritySize = code.paritySize();

        final StorageBackend backend = new SpecialBackend();
        if (code instanceof SimpleRegeneratingCode) {
            this.fed = new SimpleRegeneratingFileEncoderDecoder((SimpleRegeneratingCode) code, backend);
        } else {
            this.fed = new FileEncoderDecoder(code, backend);
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return new ArrayList<Object[]>() {{
            add(new Object[] {new ReedSolomonCode(10, 4)});
            add(new Object[] {new SimpleRegeneratingCode(10, 6, 5)});
        }};
    }

    @Test
    public void testRepair() throws TooManyErasedLocations {
        final byte[] data = FileEncoderDecoderTestUtils.createRandomBigByteBuffer();
        final int dataLength = data.length;
        final byte[] data2 = data.clone();
        final String filepath = FileEncoderDecoderTestUtils.generateRandomPath();

        this.mode = Mode.FAULTY;
        fed.writeFile(filepath, dataLength, 0, ByteBuffer.wrap(data));
        this.mode = Mode.REPAIRING;
        fed.repairAllFiles();
        this.mode = Mode.REPAIRED;

        final ByteBuffer out = ByteBuffer.allocate(dataLength);
        fed.readFile(filepath, dataLength, 0, out);

        out.rewind();

        for (int i = 0; i < dataLength; i++) {
            Assert.assertEquals("Inequality at index " + i, data2[i], out.get());
        }
    }

    private enum Mode {
        FAULTY, REPAIRING, REPAIRED
    }

    private class SpecialBackend extends MemoryStorageBackend {

        @Override
        public int storeBlock(int blockData, int position) {
            if (mode == Mode.FAULTY) {
                if (isPositionFaulty(position)) {
                    // There should not be any value at that key
                    return Integer.MAX_VALUE;
                }
            }
            return super.storeBlock(blockData, position);
        }

        @Override
        public Optional<Integer> retrieveBlock(int key) {
            if (mode == Mode.REPAIRED) {
                final int position = computePositionWithBlockKey(key);
                // After repair, reading the parity is forbidden
                if (position < paritySize) {
                    Assert.fail("A parity block has been read after repair");
                    return Optional.empty();
                }
            }
            return super.retrieveBlock(key);
        }

        private boolean isPositionFaulty(int position) {
            return position == paritySize / 2 || position == (stripeSize + paritySize) / 2;
        }
    }
}
