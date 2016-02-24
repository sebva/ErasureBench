package ch.unine.vauchers.erasuretester.backend;

import it.unimi.dsi.fastutil.longs.LongList;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Metadata stored in the backend in parallel to blocks
 */
public class FileMetadata {
    /**
     * Ordered list of blocks consisting of the file. Organized like: <br/>
     * <pre>[stripe 1 parity blocks][stripe 1 data blocks][stripe 2 parity blocks][stripe 2 data blocks], etc.</pre>
     * <br/>
     * The last stripe can contain meaningless data blocks, to always have complete stripes with parity blocks.
     */
    private LongList blockKeys;
    /**
     * Size of the real contents of the file.
     */
    private int contentsSize;
    public static final FileMetadata EMPTY_METADATA = new FileMetadata();

    /**
     * Get the ordered list of block keys associated with the file. The data associated with each block key can be
     * retrieved from the storage backend.
     * @return The list of block keys
     */
    public @NotNull Optional<LongList> getBlockKeys() {
        return Optional.ofNullable(blockKeys);
    }

    /**
     * Change the blocks associated with file. Remember to notify the storage backend after calling this method.
     * @param blockKeys The new list of block keys
     * @return This object for call chaining
     */
    public FileMetadata setBlockKeys(LongList blockKeys) {
        this.blockKeys = blockKeys;
        return this;
    }

    /**
     * Return the size of the file in the perspective of the user.
     * @return The total size of the file after decoding
     */
    public int getContentsSize() {
        return contentsSize;
    }

    /**
     * Set the size of the file in the perspective of the user.
     * @param contentsSize The new size of the file
     * @return This object for call chaining
     */
    public FileMetadata setContentsSize(int contentsSize) {
        this.contentsSize = contentsSize;
        return this;
    }
}
