package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public class FileMetadata {
    private List<String> blockKeys;
    private int contentsSize;
    public static final FileMetadata EMPTY_METADATA = new FileMetadata();

    public @NotNull Optional<List<String>> getBlockKeys() {
        return Optional.ofNullable(blockKeys);
    }

    public FileMetadata setBlockKeys(@NotNull List<String> blockKeys) {
        this.blockKeys = blockKeys;
        return this;
    }

    public int getContentsSize() {
        return contentsSize;
    }

    public FileMetadata setContentsSize(int contentsSize) {
        this.contentsSize = contentsSize;
        return this;
    }
}
