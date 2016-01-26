package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Storage backend backed by a Redis server.<br/>
 * The address of the Redis server can be configure using the <i>REDIS_ADDRESS</i> environment variable. The default
 * is to use the server running on the local machine.<br/>
 * Uses the <a href="https://github.com/xetorthio/jedis">Jedis library</a>.
 */
public class JedisStorageBackend extends StorageBackend {
    private static final String BLOCKS_PREFIX = "erasure-tester-blocks/";
    private static final String METADATA_BLOCKS_KEY = "erasure-tester-metadata-blocks/";
    private static final String METADATA_SIZE_KEY = "erasure-tester-metadata-size/";

    private JedisCommands redis;

    public JedisStorageBackend() {
        final String redis_address = System.getenv("REDIS_ADDRESS");
        if (redis_address == null) {
            redis = new Jedis();
        } else {
            System.out.println("Connecting to master at " + redis_address);
            final String[] split = redis_address.split(":");
            Set<HostAndPort> node = Stream.of(new HostAndPort(split[0], Integer.parseInt(split[1]))).collect(Collectors.toSet());
            redis = new JedisCluster(node);
        }
    }

    @Override
    public Optional<FileMetadata> getFileMetadata(@NotNull String path) {
        String blockKeysKey = METADATA_BLOCKS_KEY.concat(path);
        long blockKeysSize = redis.llen(blockKeysKey);
        int fileSize = Integer.parseInt(Optional.ofNullable(redis.get(METADATA_SIZE_KEY.concat(path))).orElse("0"));
        final List<Long> blockKeys = LongStream.range(0, blockKeysSize).map(
                (i) -> Long.parseLong(redis.lpop(blockKeysKey))).boxed().collect(Collectors.toList());

        return Optional.of(new FileMetadata().setBlockKeys(blockKeys).setContentsSize(fileSize));
    }

    @Override
    public void setFileMetadata(@NotNull String path, @NotNull FileMetadata metadata) {
        redis.set(METADATA_SIZE_KEY.concat(path), String.valueOf(metadata.getContentsSize()));
        final String blocks_key = METADATA_BLOCKS_KEY.concat(path);
        if (redis.llen(blocks_key) > 0) {
            redis.ltrim(blocks_key, 0, 0);
        }
        List<Long> blockKeys = metadata.getBlockKeys().orElseGet(Collections::<Long>emptyList);
        redis.rpush(blocks_key, blockKeys.stream().map(String::valueOf).toArray(String[]::new));
    }

    @Override
    public Optional<Integer> retrieveBlock(long key) {
        final String value = redis.get(BLOCKS_PREFIX.concat(String.valueOf(key)));
        if (value == null) {
            return Optional.empty();
        } else {
            return Optional.of(Integer.parseInt(value));
        }
    }

    @Override
    public void storeBlock(long key, int blockData) {
        redis.set(BLOCKS_PREFIX.concat(String.valueOf(key)), String.valueOf(blockData));
    }

    @Override
    public boolean isBlockAvailable(long key) {
        return redis.exists(String.valueOf(key));
    }

    @Override
    public void disconnect() {
        try {
            ((Closeable)redis).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
