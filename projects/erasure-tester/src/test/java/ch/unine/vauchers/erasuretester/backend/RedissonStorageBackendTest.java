package ch.unine.vauchers.erasuretester.backend;

public class RedissonStorageBackendTest extends StorageBackendTest<RedissonStorageBackend> {
    @Override
    protected RedissonStorageBackend createInstance() {
        return new RedissonStorageBackend();
    }
}
