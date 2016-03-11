package ch.unine.vauchers.erasuretester.backend;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public abstract class StorageBackendBenchmark {
    volatile StorageBackend sut = createBackend();

    protected abstract StorageBackend createBackend();

    @Setup
    public void setup() {
        sut.storeBlock(329564383, 42);
    }

    @Benchmark
    public void storeSync() {
        sut.storeBlock(329564383, 42);
    }

    @Benchmark
    public int retrieveSync() {
        return sut.retrieveBlock(329564383).orElseThrow(() -> new RuntimeException("key not added during setup"));
    }
}