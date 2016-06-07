package ch.unine.vauchers.erasuretester.backend;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Storage backend backed by a Redis server.<br/>
 * The address of the Redis server can be configure using the <i>REDIS_ADDRESS</i> environment variable. The default
 * is to use the server running on the local machine.<br/>
 * Uses the <a href="https://github.com/xetorthio/jedis">Jedis library</a>.
 *
 * <h3>Redis slots assignation strategy</h3>
 * When using Redis in cluster mode, the node that will host a given key is decided by applying the CRC16 function on
 * the key. By default, the redis-trib program will divide the key space evenly among nodes, in a linear fashion. E.g.
 * If there are 3 nodes, node 0 gets slots 0-5461, node 1 5462-10921 and node 2 10922-16384. We use this assumption to
 * make sure that blocks belonging to different positions do not get stored on the same node. To do this, we divide the
 * Redis slots space in totalSize. There can be more than totalSize nodes in the cluster, so the exact slot is randomized
 * using a hash function.<br/>
 * To render this strategy possible, we bypass the CRC16 function by using a table (crc-map.txt) obtained by
 * brute-forcing the algorithm. The value from this table is coded into the final key between accolades {}, so that it
 * becomes the only input to Redis' CRC16 function.
 */
public class JedisStorageBackend extends StorageBackend {
    private static final String BLOCKS_PREFIX = "blocks/";

    private final JedisCommands redis;
    private final Map<String, FileMetadata> metadataMap;
    private final HashFunction hashFunction;
    /**
     * Redis slot space divided by totalSize
     */
    private int redisSlotDelta;

    /**
     * Constructor.
     * <strong>You HAVE TO call defineTotalSize before using this object!</strong>
     */
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
    public Collection<String> getAllFilePaths() {
        return new ArrayList<>(metadataMap.keySet());
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

    private String computeRedisKey(int redisKey) {
        final int offset = Math.floorMod(hashFunction.hashInt(redisKey).asInt(), redisSlotDelta);
        final int redisSlot = computePositionWithRedisKey(redisKey) * redisSlotDelta + offset;
        final String crcHack = JedisTools.CRC16_NUMBERS_CORRESPONDANCES[redisSlot];
        return crcHack + BLOCKS_PREFIX + redisKey;
    }

    @Override
    public void clearReadCache() {
        super.clearReadCache();
        if (redis instanceof JedisCluster) {
            final JedisClusterConnectionHandler connectionHandler = ((JedisCluster) redis).getConnectionHandler();
            connectionHandler.renewSlotCache();
            connectionHandler.getNodes();
        }
    }

    @Override
    public void defineTotalSize(int totalSize) {
        super.defineTotalSize(totalSize);
        redisSlotDelta = JedisTools.REDIS_KEYS_NUMBER / totalSize;
    }
}
