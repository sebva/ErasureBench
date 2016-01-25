package ch.unine.vauchers.erasuretester.frontend;

import ch.unine.vauchers.erasuretester.erasure.FileEncoderDecoder;
import ch.unine.vauchers.erasuretester.erasure.codes.TooManyErasedLocations;
import jnr.ffi.Pointer;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Frontend exposing the implementation through a FUSE filesystem.
 *
 * Basically a copy of the
 * <a href="https://github.com/EtiennePerot/fuse-jna/blob/master/src/main/java/net/fusejna/examples/MemoryFS.java">MemoryFS</a>
 * example given by FuseJNA.
 * <br/>
 * The attributes of each file/directory are stored in memory within this class,
 * while the effective content of normal files is passed through erasure coding and stored elsewhere.
 * <br/>
 * Beware that, while the global filesystem state is destroyed on application restart, the contents of file might be
 * kept depending on the StorageBackend implementation in use.
 */
public class FuseMemoryFrontend extends FuseStubFS {
    Logger log = Logger.getLogger(FuseMemoryFrontend.class.getName());

    public FuseMemoryFrontend(FileEncoderDecoder encdec, boolean log) {
        this.encdec = encdec;
    }

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
        protected void getattr(final FileStat stat) {
            stat.st_mode.set(FileStat.S_IFDIR);
        }

        private synchronized void mkdir(final String lastComponent) {
            contents.add(new MemoryDirectory(lastComponent, this));
        }

        public synchronized void mkfile(final String lastComponent) {
            contents.add(new MemoryFile(lastComponent, this));
        }

        public synchronized void read(Pointer buf, final FuseFillDir filler) {
            for (final MemoryPath p : contents) {
                filler.apply(buf, p.name, null, 0);
            }
        }
    }

    private final class MemoryFile extends MemoryPath {
        private MemoryFile(final String name) {
            super(name);
        }

        private MemoryFile(final String name, final MemoryDirectory parent) {
            super(name, parent);
        }

        @Override
        protected void getattr(final FileStat stat) {
            stat.st_mode.set(FileStat.S_IFREG | 0777);
            stat.st_size.set(encdec.sizeOfFile(getFilepath()));
        }

        private int read(final Pointer buffer, final long size, final long offset) {
            final String filepath = getFilepath();
            try {
                encdec.readFile(filepath, (int) size, (int) offset, buffer);
                return (int) Math.min(encdec.sizeOfFile(filepath) - offset, size);
            } catch (TooManyErasedLocations e) {
                log.warning("Unable to read file at " + filepath);
                return 0;
            }
        }

        private synchronized void truncate(final long size) {
            encdec.truncate(getFilepath(), (int) size);
        }

        private int write(final Pointer buffer, final long bufSize, final long writeOffset) {
            encdec.writeFile(this.getFilepath(), (int) bufSize, (int) writeOffset, buffer);
            return (int) bufSize;
        }
    }

    private abstract class MemoryPath {
        protected String name;
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

        protected abstract void getattr(FileStat stat);

        private void rename(String newName) {
            while (newName.startsWith("/")) {
                newName = newName.substring(1);
            }
            name = newName;
        }

        public String getFilepath() {
            return this == rootDirectory ? "" : parent.getFilepath() + "/" + name;
        }
    }

    private final MemoryDirectory rootDirectory = new MemoryDirectory("");

    private final FileEncoderDecoder encdec;

    @Override
    public int access(final String path, final int access) {
        return 0;
    }

    @Override
    public int create(final String path, @mode_t long mode, final FuseFileInfo info) {
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
    public int getattr(final String path, final FileStat stat) {
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
    public int mkdir(final String path, @mode_t long mode) {
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
    public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryFile)) {
            return -ErrorCodes.EISDIR();
        }
        return ((MemoryFile) p).read(buf, size, offset);
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryDirectory)) {
            return -ErrorCodes.ENOTDIR();
        }
        ((MemoryDirectory) p).read(buf, filter);
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
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        final MemoryPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof MemoryFile)) {
            return -ErrorCodes.EISDIR();
        }
        return ((MemoryFile) p).write(buf, size, offset);
    }
}
