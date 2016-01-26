package ch.unine.vauchers.erasuretester.backend;

public class JedisStorageBackendTest extends StorageBackendTest<JedisStorageBackend> {
    @Override
    protected JedisStorageBackend createInstance() {
        return new JedisStorageBackend();
    }
}
