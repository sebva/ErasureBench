package ch.unine.vauchers.erasuretester.backend;

public class MemoryStorageBackendTest extends StorageBackendTest<MemoryStorageBackend> {
    @Override
    protected MemoryStorageBackend createInstance() {
        return new MemoryStorageBackend();
    }
}
