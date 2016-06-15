package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.JedisStorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.NullErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.ReedSolomonCode;
import ch.unine.vauchers.erasuretester.erasure.codes.SimpleRegeneratingCode;
import ch.unine.vauchers.erasuretester.utils.Utils;
import org.openjdk.jmh.annotations.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
public class FileEncoderDecoderBenchmark {
    @Param({"4000000", "16000000", "64000000"})
    public int fileSize;
    @Param({"null", "rs", "lrc"})
    public String code;

    private ByteBuffer testContents;
    private String randomPath;

    protected FileEncoderDecoder sut;
    private JedisStorageBackend storageBackend;

    @Setup
    public void setup() {
        Utils.disableLogging();

        randomPath = new BigInteger(40, new Random()).toString(256);
        testContents = ByteBuffer.allocateDirect(fileSize);
        while (testContents.hasRemaining()) {
            testContents.put((byte) (Math.random() * 256));
        }

        storageBackend = new JedisStorageBackend(true);
        final ErasureCode erasureCode;
        switch (code) {
            default:
            case "null":
                erasureCode = new NullErasureCode(10);
                break;
            case "rs":
                erasureCode = new ReedSolomonCode(10, 4);
                break;
            case "lrc":
                sut = new SimpleRegeneratingFileEncoderDecoder(new SimpleRegeneratingCode(10, 6, 5), storageBackend);
                return;
        }
        sut = new FileEncoderDecoder(erasureCode, storageBackend);
    }

    @Benchmark
    public void writeFile() {
        testContents.rewind();
        sut.writeFile(randomPath, fileSize, 0, testContents);
    }

    @TearDown
    public void cleanUp() {
        storageBackend.disconnect();
    }
}
