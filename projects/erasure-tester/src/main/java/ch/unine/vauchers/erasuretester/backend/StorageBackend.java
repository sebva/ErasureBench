package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Class that can store and retrieve file metadata and individual data blocks.
 * <br/>
 * Remember to call disconnect() after usage.
 */
public abstract class StorageBackend {
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
    public abstract Optional<Integer> retrieveBlock(long key);

    /**
     * Store a data block. If a block with the same identifier already exists, it is overwritten.
     * @param key The unique identifier of the block
     * @param blockData The data to store
     */
    public abstract void storeBlock(long key, int blockData);

    /**
     * Ask if a specified block can be retrieved.<br/>
     * If this returns false, then any retrieve method called with the same key will fail. If this returns true and a
     * retrieve method fails, then something happened in the meantime, or a bug was encountered.
     * @param key The unique identifier of the block
     * @return A boolean that specifies whether the block is available
     */
    public abstract boolean isBlockAvailable(long key);

    /**
     * Disconnect and free-up resources used by this object.
     */
    public abstract void disconnect();
}
