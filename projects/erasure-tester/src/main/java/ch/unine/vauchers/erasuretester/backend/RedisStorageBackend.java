package ch.unine.vauchers.erasuretester.backend;

import io.netty.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;
import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.RedissonClient;
import org.redisson.core.RMap;

import java.util.Map;
import java.util.Set;

/**
 * Storage backend backed by a Redis server.<br/>
 * The address of the Redis server can be configure using the <i>REDIS_ADDRESS</i> environment variable. The default
 * is to use the server running on the local machine.<br/>
 * Uses the <a href="https://github.com/mrniko/redisson">Redisson library</a>. This class extends MemoryStorageBackend
 * as Redisson exposes Redis maps as Java Map objects. Therefore, synchronous operations are identical in both
 * implementations.
 */
public class RedisStorageBackend extends MemoryStorageBackend {
    private static final String BLOCKS_MAP_NAME = "erasure-tester-blocks";
    private static final String METADATA_MAP_NAME = "erasure-tester-metadata";

    private RedissonClient redis;

    public RedisStorageBackend() {
        final String redis_address = System.getenv("REDIS_ADDRESS");
        if (redis_address == null) {
            redis = Redisson.create();
        } else {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress(redis_address);
            redis = Redisson.create(config);
        }

        blocksStorage = redis.getMap(BLOCKS_MAP_NAME);
        metadataStorage = redis.getMap(METADATA_MAP_NAME);
    }

    @Override
    public Future<Integer> retrieveBlockAsync(@NotNull String key) {
        return ((RMap<String, Integer>)blocksStorage).getAsync(key);
    }

    @Override
    public Future<Map<String, Integer>> retrieveAllBlocksAsync(@NotNull Set<String> keys) {
        return ((RMap<String, Integer>)blocksStorage).getAllAsync(keys);
    }

    @Override
    public Future<Boolean> storeBlockAsync(@NotNull String key, int blockData) {
        return ((RMap<String, Integer>)blocksStorage).fastPutAsync(key, blockData);
    }

    @Override
    public Future<Boolean> isBlockAvailableAsync(@NotNull String key) {
        return ((RMap<String, Integer>)blocksStorage).containsKeyAsync(key);
    }

    @Override
    public void disconnect() {
        redis.shutdown();
    }
}
