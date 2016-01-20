package ch.unine.vauchers.erasuretester.backend;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class FileMetadata {
    private List<String> blockKeys = Collections.emptyList();
    private int contentsSize;
    public static final FileMetadata EMPTY_METADATA = new FileMetadata();

    public List<String> getBlockKeys() {
        return blockKeys;
    }

    public FileMetadata setBlockKeys(List<String> blockKeys) {
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
