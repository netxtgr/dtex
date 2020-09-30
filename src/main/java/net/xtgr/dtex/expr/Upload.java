/**
 * 
 */

package net.xtgr.dtex.expr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.io.CopyStreamException;

import net.xtgr.dtex.expr.prof.Profile;
import net.xtgr.dtex.expr.tk.Filter;

/**
 * 
 * @author Tiger
 *
 */
public abstract class Upload extends Transmit<Path> {
    protected Upload(Profile.Conf conf) throws IOException {
        super(conf);
        if (Files.notExists(Paths.get(conf.getSource()))) {
            Files.createDirectories(Paths.get(conf.getSource()));
        }
    }

    @Override
    public boolean upload(String local, String remote) throws SocketException,
            FTPConnectionClosedException, CopyStreamException, IOException {
        if (!fileExist(local)) {
            lggr.warn("'{}' isn't exist.", local);
            return false;
        }
        try (InputStream is = Files.newInputStream(Paths.get(local));) {
            client.sendCommand("OPTS UTF8", "ON");
            client.setFileType(FTPClient.BINARY_FILE_TYPE);
            return client.storeFile(remote, is);
        }
    }

    @Override
    public boolean download(String remote, String local) throws SocketException,
            FTPConnectionClosedException, CopyStreamException, IOException {
        throw new UnsupportedOperationException("Don't support downloading.");
    }

    @Override
    public boolean delete(String local) throws IOException {
        if (!fileExist(local)) {
            lggr.warn("'{}' isn't exist.", local);
            return false;
        }
        Files.delete(Paths.get(local));
        return true;
    }

    @Override
    public boolean rename(String from, String to) throws SocketException, IOException {
        if (!fileExist(from)) {
            lggr.warn("'{}' isn't exist.", from);
            return false;
        }
        Files.move(Paths.get(from), Paths.get(to));
        return true;
    }

    @Override
    public String[] read(String local) throws SocketException, IOException {
        if (!fileExist(local)) throw new IOException(local + " dose not exist.");
        try (BufferedReader br = Files.newBufferedReader(Paths.get(local))) {
            ArrayList<String> tmps = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) tmps.add(line);
            return tmps.toArray(new String[tmps.size()]);
        }
    }

    @Override
    public Path[] list(String file, Filter<Path> filter) throws java.io.IOException {
        if (Files.notExists(Paths.get(file))) {
            lggr.warn("'{}' isn't exist.", file);
            return new Path[0];
        }
        java.util.List<Path> result = new java.util.ArrayList<>();
        Files.walkFileTree(Paths.get(file), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                if (!Files.isSameFile(dir, Paths.get(source)) && Files.isDirectory(dir))
                    return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (filter.accept(file)) result.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return result.toArray(new Path[result.size()]);
    }

    @Override
    public boolean dirExist(String local) throws IOException {
        return localDirExist(local);
    }

    @Override
    public boolean fileExist(String local) throws IOException {
        return localFileExist(local);
    }

}
