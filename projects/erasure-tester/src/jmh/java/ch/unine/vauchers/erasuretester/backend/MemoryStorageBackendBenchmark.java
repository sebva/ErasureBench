package ch.unine.vauchers.erasuretester.backend;

/**
 *
 */
public class MemoryStorageBackendBenchmark extends StorageBackendBenchmark {
    protected StorageBackend createBackend() {
        return new MemoryStorageBackend(new NullFailureGenerator());
    }
}
