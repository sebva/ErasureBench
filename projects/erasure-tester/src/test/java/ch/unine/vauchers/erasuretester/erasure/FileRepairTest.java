package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.*;
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
    private final StorageBackend backend;

    public FileRepairTest(ErasureCode code) {
        this.mode = Mode.FAULTY;
        this.stripeSize = code.stripeSize();
        this.paritySize = code.paritySize();

        backend = new SpecialBackend();
        if (code instanceof SimpleRegeneratingCode) {
            this.fed = new SimpleRegeneratingFileEncoderDecoder((SimpleRegeneratingCode) code, backend);
        } else {
            this.fed = new FileEncoderDecoder(code, backend);
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return new ArrayList<Object[]>() {{
            add(new Object[] {new XORCode(2, 1)});
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
        backend.clearReadCache();
        this.mode = Mode.REPAIRING;
        fed.repairFile(filepath);
        backend.clearReadCache();
        this.mode = Mode.REPAIRED;

        Assert.assertTrue(backend.getFileMetadata(filepath).get().getBlockKeys().get().parallelStream().noneMatch(key -> key == Integer.MAX_VALUE));

        final ByteBuffer out = ByteBuffer.allocate(dataLength);
        fed.readFile(filepath, dataLength, 0, out);

        out.rewind();

        for (int i = 0; i < dataLength; i++) {
            Assert.assertEquals("Inequality at index " + i, data2[i], out.get());
        }
    }

    @Test
    public void testRepairMultipleFiles() throws TooManyErasedLocations {
        final int nbFiles = 1000;
        final byte[][] data = new byte[nbFiles][];
        final byte[][] data2 = new byte[nbFiles][];
        final String[] filepaths = new String[nbFiles];
        for (int i = 0; i < nbFiles; i++) {
            data[i] = FileEncoderDecoderTestUtils.createRandomBigByteBuffer();
            data2[i] = data[i].clone();
            filepaths[i] = FileEncoderDecoderTestUtils.generateRandomPath();
        }

        this.mode = Mode.FAULTY;
        for (int i = 0; i < nbFiles; i++) {
            fed.writeFile(filepaths[i], data[i].length, 0, ByteBuffer.wrap(data[i]));
        }
        backend.clearReadCache();
        this.mode = Mode.REPAIRING;
        fed.repairAllFiles();
        backend.clearReadCache();
        this.mode = Mode.REPAIRED;

        for (int i = 0; i < nbFiles; i++) {
            final String filepath = filepaths[i];
            Assert.assertTrue(backend.getFileMetadata(filepath).get().getBlockKeys().get().parallelStream().noneMatch(key -> key == Integer.MAX_VALUE));

            final ByteBuffer out = ByteBuffer.allocate(data2.length);
            fed.readFile(filepath, data2.length, 0, out);
            out.rewind();

            for (int j = 0; j < data2.length; j++) {
                Assert.assertEquals("Inequality at index " + j, data2[i][j], out.get());
            }
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
            if (paritySize == 1) return position == 1;
            return position == paritySize / 2 || position ==  paritySize + (stripeSize / 2);
        }
    }
}
