package ch.unine.vauchers.erasuretester.backend;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Base64;

/**
 * Container for many blocks that will be stored in the key-value store.
 * Has a fixed size.
 */
public class BlocksContainer implements Serializable {
    private IntArrayList blocks;
    private final int bufferSize;

    /**
     * Constructor
     * @param bufferSize The amount of blocks that can be stored. Check isFull.
     */
    public BlocksContainer(int bufferSize) {
        this.bufferSize = bufferSize;
        blocks = new IntArrayList(bufferSize);
    }

    private BlocksContainer(int bufferSize, IntArrayList blocks) {
        this.bufferSize = bufferSize;
        this.blocks = blocks;
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
            DataInputStream dis = new DataInputStream(bais);
            int bufferSize = dis.readInt();

            int elementsLength = dis.readInt();
            final IntArrayList blocks = new IntArrayList(bufferSize);
            for (int i = 0; i < elementsLength; i++) {
                blocks.add(dis.readInt());
            }

            return new BlocksContainer(bufferSize, blocks);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String toString(@NotNull BlocksContainer container) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            final IntArrayList blocks = container.blocks;
            final int elementsLength = blocks.size();

            dos.writeInt(container.bufferSize);
            dos.writeInt(elementsLength);
            for (int i = 0; i < elementsLength; i++) {
                dos.writeInt(blocks.getInt(i));
            }
            dos.flush();

            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
