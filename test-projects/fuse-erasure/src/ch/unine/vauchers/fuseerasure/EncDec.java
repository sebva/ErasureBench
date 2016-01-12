package ch.unine.vauchers.fuseerasure;

import java.nio.ByteBuffer;

public class EncDec {

    public static int read(ByteBuffer out, long offset, long size, MemoryFS.MemoryFile file) {
        final int bytesToRead = (int) Math.min(file.contents.capacity() - offset, size);
        final byte[] bytesRead = new byte[bytesToRead];
        synchronized (file) {
            file.contents.position((int) offset);
            file.contents.get(bytesRead, 0, bytesToRead);
            out.put(bytesRead);
            file.contents.position(0); // Rewind
        }
        return bytesToRead;
    }

    public static int write(ByteBuffer buffer, long bufSize, long writeOffset, MemoryFS.MemoryFile file) {
        final int maxWriteIndex = (int) (writeOffset + bufSize);
        final byte[] bytesToWrite = new byte[(int) bufSize];
        synchronized (file) {
            if (maxWriteIndex > file.contents.capacity()) {
                // Need to create a new, larger buffer
                final ByteBuffer newContents = ByteBuffer.allocate(maxWriteIndex);
                newContents.put(file.contents);
                file.contents = newContents;
            }
            buffer.get(bytesToWrite, 0, (int) bufSize);
            file.contents.position((int) writeOffset);
            file.contents.put(bytesToWrite);
            file.contents.position(0); // Rewind
        }
        return (int) bufSize;
    }
}
