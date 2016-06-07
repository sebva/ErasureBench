package ch.unine.vauchers.erasuretester.backend;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Base64;

/**
 * Container for many blocks that will be stored in the key-value store.
 * Has a fixed size.
 */
public class BlocksContainer implements Serializable {
    private IntList blocks;
    private final int bufferSize;

    /**
     * Constructor
     * @param bufferSize The amount of blocks that can be stored. Check isFull.
     */
    public BlocksContainer(int bufferSize) {
        this.bufferSize = bufferSize;
        blocks = new IntArrayList(bufferSize);
    }

    public int get(int key) {
        return blocks.getInt(key);
    }

    public void put(int blockData) {
        blocks.add(blockData);
    }

    public boolean isFull() {
        return blocks.size() == bufferSize;
    }

    public static BlocksContainer fromString(String serialize) {
        try {
            byte[] bytes = Base64.getDecoder().decode(serialize);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (BlocksContainer) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String toString(@NotNull BlocksContainer container) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(container);
            oos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
