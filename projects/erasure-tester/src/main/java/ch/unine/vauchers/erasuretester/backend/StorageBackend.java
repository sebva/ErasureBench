package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Class that can store and retrieve file metadata and individual data blocks.
 * <br/>
 * Remember to call disconnect() after usage.
 */
public abstract class StorageBackend {
    public static final int BUFFER_SIZE = 9363;
    private static final int CACHE_SIZE = 50;
    private BlocksContainer[] writeBuffers;
    private LinkedHashMap<Long, BlocksContainer> readCache;
    private long[] counters;
    private int totalSize;

    /**
     * You NEED to call defineTotalSize before any other action on this object!
     */
    public StorageBackend() {
        readCache = new LinkedHashMap<Long, BlocksContainer>(CACHE_SIZE + 1, .75f, true) {
            @Override
            public boolean removeEldestEntry(Map.Entry<Long, BlocksContainer> eldest) {
                return size() > CACHE_SIZE;
            }
        };
    }

    /**
     * Get the metadata object associated with a file
     * @param path A string uniquely identifying the file
     * @return The metadata object wrapped in an Optional (not present if not found)
     */
    public abstract Optional<FileMetadata> getFileMetadata(@NotNull String path);

    /**
     * Set and store the metadata object associated with a file
     * @param path A string uniquely identifying the file
     * @param metadata The new metadata object
     */
    public abstract void setFileMetadata(@NotNull String path, @NotNull FileMetadata metadata);

    /**
     * Retrieve a data block from storage
     * @param key The unique identifier of the block
     * @return The block wrapped in an Optional (not present if not found)
     */
    public Optional<Integer> retrieveBlock(long key) {
        long redisKey = key / BUFFER_SIZE;
        BlocksContainer container = readCache.get(redisKey);
        if (container == null) {
            container = fetchAndCache(redisKey);
        }
        if (container != null) {
            return Optional.of(container.get((int) (key % BUFFER_SIZE)));
        } else {
            return Optional.empty();
        }
    }

    private BlocksContainer fetchAndCache(long redisKey) {
        final Optional<String> optionalContainer = retrieveAggregatedBlocks(redisKey);
        if (!optionalContainer.isPresent()) {
            return null;
        } else {
            BlocksContainer container = BlocksContainer.fromString(optionalContainer.get());
            readCache.put(redisKey, container);
            return container;
        }
    }

    public abstract Optional<String> retrieveAggregatedBlocks(long key);

    protected abstract void storeAggregatedBlocks(long key, String blockData);

    /**
     * Store a data block. If a block with the same identifier already exists, it is overwritten.
     * @param blockData The data to store
     * @param position Position in [0; (stripeSize + paritySize)]. Used to effectively distribute the load on nodes.
     * @return The unique identifier of the block
     */
    public long storeBlock(int blockData, int position) {
        long key = counters[position];

        writeBuffers[position].put(blockData);

        if (writeBuffers[position].isFull()) {
            flush(position);
        } else {
            counters[position]++;
        }

        return key;
    }

    private void flush(int position) {
        String aggregatedBlocks = BlocksContainer.toString(writeBuffers[position]);
        writeBuffers[position] = new BlocksContainer();
        final long counter = counters[position];
        final long redisKey = counter / BUFFER_SIZE;
        storeAggregatedBlocks(redisKey, aggregatedBlocks);

        counters[position] = redisKey * BUFFER_SIZE + totalSize * BUFFER_SIZE;
    }

    /**
     * Ask if a specified block can be retrieved.<br/>
     * If this returns false, then any retrieve method called with the same key will fail. If this returns true and a
     * retrieve method fails, then something happened in the meantime, or a bug was encountered.
     * @param key The unique identifier of the block
     * @return A boolean that specifies whether the block is available
     */
    public boolean isBlockAvailable(long key) {
        long redisKey = key / BUFFER_SIZE;
        return readCache.containsKey(redisKey) || fetchAndCache(redisKey) != null;
    }

    protected abstract boolean isAggregatedBlockAvailable(long key);

    /**
     * Disconnect and free-up resources used by this object.
     */
    public abstract void disconnect();

    public void defineTotalSize(int totalSize) {
        this.totalSize = totalSize;
        writeBuffers = new BlocksContainer[totalSize];
        counters = new long[totalSize];

        for (int i = 0; i < totalSize; i++) {
            writeBuffers[i] = new BlocksContainer();
            counters[i] = i * BUFFER_SIZE;
        }
    }

    public void flushAll() {
        for (int i = 0; i < totalSize; i++) {
            flush(i);
        }
    }
}
