package ch.unine.vauchers.erasuretester.backend;

import java.lang.reflect.Field;
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

    public void setBlockKeys(List<String> blockKeys) {
        this.blockKeys = blockKeys;
    }

    public int getContentsSize() {
        return contentsSize;
    }

    public void setContentsSize(int contentsSize) {
        this.contentsSize = contentsSize;
    }
}
