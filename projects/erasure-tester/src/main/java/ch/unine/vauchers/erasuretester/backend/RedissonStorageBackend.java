package ch.unine.vauchers.erasuretester.backend;

import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.RedissonClient;

/**
 * Storage backend backed by a Redis server.<br/>
 * The address of the Redis server can be configured using the <i>REDIS_ADDRESS</i> environment variable. The default
 * is to use the server running on the local machine.<br/>
 * Uses the <a href="https://github.com/mrniko/redisson">Redisson library</a>. This class extends MemoryStorageBackend
 * as Redisson exposes Redis maps as Java Map objects. Therefore, synchronous operations are identical in both
 * implementations.
 *
 * We recommend to use the Jedis implementation instead of this one. It is faster and balances the blocks better in
 * clustered mode.
 */
public class RedissonStorageBackend extends MemoryStorageBackend {
    private static final String BLOCKS_MAP_NAME = "erasure-tester-blocks";
    private static final String METADATA_MAP_NAME = "erasure-tester-metadata";

    private RedissonClient redis;

    public RedissonStorageBackend() {
        final String redis_address = System.getenv("REDIS_ADDRESS");
        if (redis_address == null) {
            redis = Redisson.create();
        } else {
            System.out.println("Connecting to master at " + redis_address);
            Config config = new Config();
            config.useClusterServers()
                    .addNodeAddress(redis_address);
            redis = Redisson.create(config);
        }

        blocksStorage = redis.getMap(BLOCKS_MAP_NAME);
        metadataStorage = redis.getMap(METADATA_MAP_NAME);
    }

    @Override
    public void disconnect() {
        redis.shutdown();
    }
}
