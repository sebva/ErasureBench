package ch.unine.vauchers.erasuretester.backend;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Class that can store and retrieve file metadata and individual data blocks.
 * <br/>
 * Remember to call disconnect() after usage.
 */
public abstract class StorageBackend {
    protected int bufferSize;
    public static final int FUSE_READ_SIZE = 1024 * 128 + 20; // Update accordingly
    public static final int CACHE_SIZE = 50;
    private BlocksContainer[] writeBuffers;
    private LinkedHashMap<Integer, BlocksContainer> readCache;
    private int[] counters;
    protected int totalSize;
    private final IntSet negativeCache;

    /**
     * You NEED to call defineTotalSize before any other action on this object!
     */
    public StorageBackend() {
        readCache = new LinkedHashMap<Integer, BlocksContainer>(CACHE_SIZE + 1, .75f, true) {
            @Override
            public boolean removeEldestEntry(Map.Entry<Integer, BlocksContainer> eldest) {
                return size() > CACHE_SIZE;
            }
        };
        negativeCache = new IntOpenHashSet();
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
     * @param key The unique identifier of the block, given by storeBlock
     * @return The block wrapped in an Optional (not present if not found)
     */
    public Optional<Integer> retrieveBlock(int key) {
        int redisKey = key / bufferSize;
        BlocksContainer container = readCache.get(redisKey);
        if (container == null) {
            container = fetchAndCache(redisKey);
        }
        if (container != null) {
            return Optional.of(container.get(key % bufferSize));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Fetch the aggregated block from the backend and cache it in readCache
     * @param redisKey
     * @return The corresponding block, or null
     */
    @Nullable
    private BlocksContainer fetchAndCache(int redisKey) {
        final Optional<String> optionalContainer = retrieveAggregatedBlocks(redisKey);
        if (!optionalContainer.isPresent()) {
            negativeCache.add(redisKey);
            return null;
        } else {
            BlocksContainer container = BlocksContainer.fromString(optionalContainer.get());
            readCache.put(redisKey, container);
            return container;
        }
    }

    /**
     * Retrieve an aggregation of blocks in String form (to deserialize)
     * @param key The key
     * @return The String wrapped in an Optional
     */
    protected abstract Optional<String> retrieveAggregatedBlocks(int key);

    /**
     * Store a serialized aggregation of blocks
     * @param key The key
     * @param blockData The data
     */
    protected abstract void storeAggregatedBlocks(int key, String blockData);

    /**
     * Store a data block. This returns a key to later ask for the data.
     * @param blockData The data to store
     * @param position Position in [0; (stripeSize + paritySize)]. Used to effectively distribute the load on nodes.
     * @return The unique identifier of the block
     */
    public int storeBlock(int blockData, int position) {
        int key = counters[position];

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
        writeBuffers[position] = new BlocksContainer(bufferSize);
        final int counter = counters[position];
        final int redisKey = counter / bufferSize;
        storeAggregatedBlocks(redisKey, aggregatedBlocks);

        counters[position] = redisKey * bufferSize + totalSize * bufferSize;
    }

    /**
     * Ask if a specified block can be retrieved.<br/>
     * If this returns false, then any retrieve method called with the same key will fail. If this returns true and a
     * retrieve method fails, then something happened in the meantime, or a bug was encountered.
     * @param key The unique identifier of the block
     * @return A boolean that specifies whether the block is available
     */
    public boolean isBlockAvailable(int key) {
        int redisKey = key / bufferSize;
        return !negativeCache.contains(redisKey) && (readCache.containsKey(redisKey) || fetchAndCache(redisKey) != null);
    }

    protected abstract boolean isAggregatedBlockAvailable(int key);

    public int computePositionWithBlockKey(int key) {
        return Math.floorMod(key / bufferSize, totalSize);
    }

    protected int computePositionWithRedisKey(int redisKey) {
        return Math.floorMod(redisKey, totalSize);
    }

    /**
     * Disconnect and free-up resources used by this object.
     */
    public abstract void disconnect();

    /**
     * Set the total size (stripe size + parity size) to use.
     * This method MUST be called before any usage of the object.
     * @param totalSize The total size (stripe size + parity size)
     */
    public void defineTotalSize(int totalSize) {
        this.totalSize = totalSize;
        writeBuffers = new BlocksContainer[totalSize];
        counters = new int[totalSize];
        bufferSize = (int) Math.ceil(FUSE_READ_SIZE / (double) totalSize);

        for (int i = 0; i < totalSize; i++) {
            writeBuffers[i] = new BlocksContainer(bufferSize);
            counters[i] = i * bufferSize;
        }
    }

    /**
     * Force write all temporary blocks to the storage backend.
     */
    public void flushAll() {
        for (int i = 0; i < totalSize; i++) {
            flush(i);
        }
    }

    public void clearReadCache() {
        readCache.clear();
        negativeCache.clear();
    }
}
