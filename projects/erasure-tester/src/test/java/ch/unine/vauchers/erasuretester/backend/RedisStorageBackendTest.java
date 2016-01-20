package ch.unine.vauchers.erasuretester.backend;

public class RedisStorageBackendTest extends StorageBackendTest<RedisStorageBackend> {
    @Override
    protected RedisStorageBackend createInstance() {
        return new RedisStorageBackend();
    }
}
