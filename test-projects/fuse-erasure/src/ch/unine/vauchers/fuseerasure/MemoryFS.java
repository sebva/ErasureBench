package ch.unine.vauchers.fuseerasure;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.FuseException;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterAssumeImplemented;

public class MemoryFS extends FuseFilesystemAdapterAssumeImplemented {
    private final class MemoryDirectory extends MemoryPath {
        private final List<MemoryPath> contents = new ArrayList<MemoryPath>();

        private MemoryDirectory(final String name) {
            super(name);
        }

        private MemoryDirectory(final String name, final MemoryDirectory parent) {
            super(name, parent);
        }

        public synchronized void add(final MemoryPath p) {
            contents.add(p);
            p.parent = this;
        }

        private synchronized void deleteChild(final MemoryPath child) {
            contents.remove(child);
        }

        @Override
        protected MemoryPath find(String path) {
            if (super.find(path) != null) {
                return super.find(path);
            }
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            synchronized (this) {
                if (!path.contains("/")) {
                    for (final MemoryPath p : contents) {
                        if (p.name.equals(path)) {
                            return p;
                        }
                    }
                    return null;
                }
                final String nextName = path.substring(0, path.indexOf("/"));
                final String rest = path.substring(path.indexOf("/"));
                for (final MemoryPath p : contents) {
                    if (p.name.equals(nextName)) {
                        return p.find(rest);
                    }
                }
            }
            return null;
        }

        @Override
        protected void getattr(final StatWrapper stat) {
            stat.setMode(NodeType.DIRECTORY);
        }

        private synchronized void mkdir(final String lastComponent) {
            contents.add(new MemoryDirectory(lastComponent, this));
        }

        public synchronized void mkfile(final String lastComponent) {
            contents.add(new MemoryFile(lastComponent, this));
        }

        public synchronized void read(final DirectoryFiller filler) {
            for (final MemoryPath p : contents) {
                filler.add(p.name);
            }
        }
    }

    final class MemoryFile extends MemoryPath {
        ByteBuffer contents = ByteBuffer.allocate(0);

        private MemoryFile(final String name) {
            super(name);
        }

        private MemoryFile(final String name, final MemoryDirectory parent) {
            super(name, parent);
        }

        public MemoryFile(final String name, final String text) {
            super(name);
            try {
                final byte[] contentBytes = text.getBytes("UTF-8");
                contents = ByteBuffer.wrap(contentBytes);
            } catch (final UnsupportedEncodingException e) {
                // Not going to happen
            }
        }

        @Override
        protected void getattr(final StatWrapper stat) {
            stat.setMode(NodeType.FILE).size(contents.capacity());
        }

        private int read(final ByteBuffer buffer, final long size, final long offset) {
            return EncDec.read(buffer, offset, size, this);
        }

        private synchronized void truncate(final long size) {
            if (size < contents.capacity()) {
                // Need to create a new, smaller buffer
                final ByteBuffer newContents = ByteBuffer.allocate((int) size);
                final byte[] bytesRead = new byte[(int) size];
                contents.get(bytesRead);
                newContents.put(bytesRead);
                contents = newContents;
            }
        }

        private int write(final ByteBuffer buffer, final long bufSize, final long writeOffset) {
            return EncDec.write(buffer, bufSize, writeOffset, this);
        }
    }

    private abstract class MemoryPath {
        private String name;
        private MemoryDirectory parent;

        private MemoryPath(final String name) {
            this(name, null);
        }

        private MemoryPath(final String name, final MemoryDirectory parent) {
            this.name = name;
            this.parent = parent;
        }

        private synchronized void delete() {
            if (parent != null) {
                parent.deleteChild(this);
                parent = null;
            }
        }

        protected MemoryPath find(String path) {
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.equals(name) || path.isEmpty()) {
                return this;
            }
            return null;
        }

        protected abstract void getattr(StatWrapper stat);

        private void rename(String newName) {
            while (newName.startsWith("/")) {
                newName = newName.substring(1);
            }
            name = newName;
        }
    }

    public static void main(final String... args) throws FuseException {
        if (args.length != 1) {
            System.err.println("Usage: MemoryFS <mountpoint>");
            System.exit(1);
        }
        new MemoryFS().log(true).mount(args[0]);
    }

    private final MemoryDirectory rootDirectory = new MemoryDirectory("");

    @Override
    public int access(final String path, final int access) {
        return 0;
    }

    @Override
    public int create(final String path, final ModeWrapper mode, final FileInfoWrapper info) {
        if (getPath(path) != null) {
            return -ErrorCodes.EEXIST();
        }
        final MemoryPath parent = getParentPath(path);
        if (parent instanceof MemoryDirectory) {
            ((MemoryDirectory) parent).mkfile(getLastComponent(path));
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int getattr(final String path, final StatWrapper stat) {
        final MemoryPath p = getPath(path);
        if (p != null) {
            p.getattr(stat);
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    private String getLastComponent(String path) {
        while (path.substring(path.length() - 1).equals("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty()) {
            return "";
        }
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private MemoryPath getParentPath(final String path) {
        return rootDirectory.find(path.substring(0, path.lastIndexOf("/")));
    }

    private MemoryPath getPath(final String path) {
        return rootDirectory.find(path);
    }

    @Override
    public int mkdir(final String path, final ModeWrapper mode) {
        if (getPath(path) != null) {
            return -ErrorCodes.EEXIST();
        }
        final MemoryPath parent = getParentPath(path);
        if (parent instanceof MemoryDirectory) {
            ((MemoryDirectory) parent).mkdir(getLastComponent(path));
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int open(final String path, final FileInfoWrapper info) {
        return 0;
    }

    @Override
    public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final FileInfoWrapper info) {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryFile)) {
            return -ErrorCodes.EISDIR();
        }
        return ((MemoryFile) p).read(buffer, size, offset);
    }

    @Override
    public int readdir(final String path, final DirectoryFiller filler) {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryDirectory)) {
            return -ErrorCodes.ENOTDIR();
        }
        ((MemoryDirectory) p).read(filler);
        return 0;
    }

    @Override
    public int rename(final String path, final String newName) {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        final MemoryPath newParent = getParentPath(newName);
        if (newParent == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(newParent instanceof MemoryDirectory)) {
            return -ErrorCodes.ENOTDIR();
        }
        p.delete();
        p.rename(newName.substring(newName.lastIndexOf("/")));
        ((MemoryDirectory) newParent).add(p);
        return 0;
    }

    @Override
    public int rmdir(final String path) {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryDirectory)) {
            return -ErrorCodes.ENOTDIR();
        }
        p.delete();
        return 0;
    }

    @Override
    public int truncate(final String path, final long offset) {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryFile)) {
            return -ErrorCodes.EISDIR();
        }
        ((MemoryFile) p).truncate(offset);
        return 0;
    }

    @Override
    public int unlink(final String path) {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        p.delete();
        return 0;
    }

    @Override
    public int write(final String path, final ByteBuffer buf, final long bufSize, final long writeOffset,
                     final FileInfoWrapper wrapper) {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryFile)) {
            return -ErrorCodes.EISDIR();
        }
        return ((MemoryFile) p).write(buf, bufSize, writeOffset);
    }
}
