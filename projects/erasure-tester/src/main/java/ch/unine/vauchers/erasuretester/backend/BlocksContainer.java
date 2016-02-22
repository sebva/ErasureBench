package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;

/**
 *
 */
public class BlocksContainer implements Serializable {
    private ArrayList<Integer> blocks;

    public BlocksContainer() {
        blocks = new ArrayList<>(StorageBackend.BUFFER_SIZE);
    }

    public int get(int key) {
        return blocks.get(key);
    }

    public void put(int blockData) {
        blocks.add(blockData);
    }

    public boolean isFull() {
        return blocks.size() == StorageBackend.BUFFER_SIZE;
    }

    @NotNull
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
