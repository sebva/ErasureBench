package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.Utils;
import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.backend.NullFailureGenerator;
import ch.unine.vauchers.erasuretester.erasure.codes.NullErasureCode;
import org.openjdk.jmh.annotations.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
public class FileEncoderDecoderBenchmark {
    @Param({"1", "1000", "2000", "10000"})
    public int fileSize;

    private ByteBuffer testContents;
    private String randomPath;

    protected FileEncoderDecoder sut;

    @Setup
    public void setup() {
        Utils.disableLogging();

        randomPath = new BigInteger(40, new Random()).toString(256);
        testContents = ByteBuffer.allocate(fileSize);
        while (testContents.hasRemaining()) {
            testContents.put((byte) (Math.random() * 256));
        }

        sut = new FileEncoderDecoder(new NullErasureCode(10), new MemoryStorageBackend(new NullFailureGenerator()));
    }

    @Benchmark
    public void writeFile() {
        sut.writeFile(randomPath, testContents);
    }
}
