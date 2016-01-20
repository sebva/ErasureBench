package ch.unine.vauchers.erasuretester.backend;

/**
 *
 */
public class RedisStorageBackendBenchmark extends StorageBackendBenchmark {
    protected StorageBackend createBackend() {
        return new MemoryStorageBackend(new NullFailureGenerator());
    }
}
