package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.JedisStorageBackend;
import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.*;
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

    private ByteBuffer outBuffer;
    private String randomPath;

    protected FileEncoderDecoder sut;
    private StorageBackend storageBackend;

    @Setup
    public void setup() {
        Utils.disableLogging();

        randomPath = new BigInteger(40, new Random()).toString(256);
        ByteBuffer testContents = ByteBuffer.allocateDirect(fileSize);
        outBuffer = ByteBuffer.allocateDirect(fileSize);
        while (testContents.hasRemaining()) {
            testContents.put((byte) (Math.random() * 256));
        }

        storageBackend = new JedisStorageBackend(true);
        final ErasureCode erasureCode;
        switch (code) {
            default:
            case "null":
                sut = new FileEncoderDecoder(new NullErasureCode(10), storageBackend);
                break;
            case "rs":
                sut = new FileEncoderDecoder(new ReedSolomonCode(10, 4), storageBackend);
                break;
            case "lrc":
                sut = new SimpleRegeneratingFileEncoderDecoder(new SimpleRegeneratingCode(10, 6, 5), storageBackend);
                break;
        }

        testContents.rewind();
        sut.writeFile(randomPath, fileSize, 0, testContents);
    }

    @Benchmark
    public void readFile() throws TooManyErasedLocations {
        outBuffer.rewind();
        sut.readFile(randomPath, fileSize, 0, outBuffer);
    }

    @TearDown
    public void cleanUp() {
        storageBackend.disconnect();
    }
}
