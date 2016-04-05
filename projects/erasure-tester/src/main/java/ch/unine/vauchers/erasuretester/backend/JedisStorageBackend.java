package ch.unine.vauchers.erasuretester.backend;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Storage backend backed by a Redis server.<br/>
 * The address of the Redis server can be configure using the <i>REDIS_ADDRESS</i> environment variable. The default
 * is to use the server running on the local machine.<br/>
 * Uses the <a href="https://github.com/xetorthio/jedis">Jedis library</a>.
 */
public class JedisStorageBackend extends StorageBackend {
    private static final String BLOCKS_PREFIX = "blocks/";

    private final JedisCommands redis;
    private final Map<String, FileMetadata> metadataMap;
    private final HashFunction hashFunction;

    public JedisStorageBackend(boolean is_cluster) {
        JedisTools.initialize();

        final String redis_address = System.getenv("REDIS_ADDRESS");
        if (redis_address == null) {
            redis = new Jedis();
        } else {
            System.out.println("Connecting to master at " + redis_address);
            final String[] split = redis_address.split(":");
            final String host = split[0];
            final int port = Integer.parseInt(split[1]);
            if (is_cluster) {
                Set<HostAndPort> node = Stream.of(new HostAndPort(host, port)).collect(Collectors.toSet());
                redis = new JedisCluster(node);
            } else {
                redis = new Jedis(host, port);
            }
        }
        hashFunction = Hashing.murmur3_32();
        metadataMap = new HashMap<>();
    }

    @Override
    public Optional<FileMetadata> getFileMetadata(@NotNull String path) {
        return Optional.ofNullable(metadataMap.get(path));
    }

    @Override
    public void setFileMetadata(@NotNull String path, @NotNull FileMetadata metadata) {
        metadataMap.put(path, metadata);
    }

    @Override
    public Optional<String> retrieveAggregatedBlocks(int key) {
        final String value = redis.get(computeRedisKey(key));
        return Optional.ofNullable(value);
    }

    @Override
    protected void storeAggregatedBlocks(int key, String blockData) {
        redis.set(computeRedisKey(key), blockData);
    }

    @Override
    public boolean isAggregatedBlockAvailable(int key) {
        return redis.exists(computeRedisKey(key));
    }

    @Override
    public void disconnect() {
        try {
            ((Closeable)redis).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String computeRedisKey(int key) {
        final int redisSlot = Math.floorMod(hashFunction.hashInt(key).asInt(), JedisTools.REDIS_KEYS_NUMBER);
        final String crcHack = JedisTools.CRC16_NUMBERS_CORRESPONDANCES[redisSlot];
        return "{" + crcHack + "}" + BLOCKS_PREFIX + key;
    }
}
