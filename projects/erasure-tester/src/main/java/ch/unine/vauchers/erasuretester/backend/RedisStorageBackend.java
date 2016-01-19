package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;
import org.redisson.Redisson;
import org.redisson.RedissonClient;

public class RedisStorageBackend extends MemoryStorageBackend {
    private static final String BLOCKS_MAP_NAME = "erasure-tester-blocks";
    private static final String METADATA_MAP_NAME = "erasure-tester-metadata";

    private RedissonClient redis;

    public RedisStorageBackend(@NotNull FailureGenerator failureGenerator) {
        super(failureGenerator);

        redis = Redisson.create();

        blocksStorage = redis.getMap(BLOCKS_MAP_NAME);
        metadataStorage = redis.getMap(METADATA_MAP_NAME);
    }

    @Override
    protected boolean isBlockFailed(@NotNull String key) {
        return !blocksStorage.containsKey(key);
    }
}
