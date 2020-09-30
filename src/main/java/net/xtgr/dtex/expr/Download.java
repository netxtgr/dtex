/**
 * 
 */

package net.xtgr.dtex.expr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.io.CopyStreamException;

import net.xtgr.dtex.expr.prof.Profile;
import net.xtgr.dtex.expr.tk.Filter;

/**
 * 
 * @author Tiger
 *
 */
public abstract class Download extends Transmit<FTPFile> {
    public Download(Profile.Conf conf) throws IOException {
        super(conf);
        if (Files.notExists(Paths.get(conf.getTarget()))) {
            Files.createDirectories(Paths.get(conf.getTarget()));
        }
    }

    @Override
    public boolean upload(String local, String remote) throws SocketException,
            FTPConnectionClosedException, CopyStreamException, IOException {
        throw new UnsupportedOperationException("Don't support uploading.");
    }

    @Override
    public boolean download(
            String remote, String local) throws SocketException,
            FTPConnectionClosedException, CopyStreamException, IOException {
        if (!fileExist(remote)) {
            lggr.warn("'{}' isn't exist.", remote);
            return false;
        }
        try (OutputStream os = Files.newOutputStream(Paths.get(local));) {
            client.sendCommand("OPTS UTF8", "ON");
            client.setFileType(FTPClient.BINARY_FILE_TYPE);
            lggr.debug("[Prepare]:+ to download:'{}'", remote);
            return client.retrieveFile(remote, os);
        }
    }

    @Override
    public boolean delete(String remote)
            throws SocketException, FTPConnectionClosedException, IOException {
        if (!fileExist(remote)) {
            lggr.warn("'{}' isn't exist.", remote);
            return false;
        }
        client.sendCommand("OPTS UTF8", "ON");
        return client.deleteFile(remote);
    }

    @Override
    public boolean rename(String from, String to)
            throws SocketException, FTPConnectionClosedException, IOException {
        if (!fileExist(from)) {
            lggr.warn("'{}' isn't exist.", from);
            return false;
        }
        String dir = toRemoteRealPath(Paths.get(to).getParent().toString());
        if (!dirExist(dir)) mkdirs(dir);
        client.sendCommand("OPTS UTF8", "ON");
        return client.rename(from, to);
    }

    @Override
    public String[] read(String remote)
            throws SocketException, FTPConnectionClosedException, IOException {
        if (!fileExist(remote)) throw new IOException(remote + " is not exist.");
        IOException ioe = null;
        client.sendCommand("OPTS UTF8", "ON");
        client.setFileType(FTPClient.BINARY_FILE_TYPE);
        InputStream is = client.retrieveFileStream(remote);
        if (Objects.isNull(is)) ioe = new IOException(remote + " can't be retrieved.");
        else if (!client.completePendingCommand()) {// !!! Must be called. It is
                                                    // used to close Socket.
            String ms = "Read the file of " + remote + " is failed.";
            ioe = new IOException(ms);
        }
        if (Objects.nonNull(ioe)) {
            is.close();
            throw ioe;
        }
        //
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
            ArrayList<String> tmps = new ArrayList<>();
            String line = null;
            while ((line = br.readLine()) != null) tmps.add(line);
            return tmps.toArray(new String[tmps.size()]);
        }
    }

    @Override
    public FTPFile[] list(String remote, Filter<FTPFile> filter)
            throws SocketException, FTPConnectionClosedException, IOException {
        ArrayList<FTPFile> fs = new ArrayList<>();
        FTPFile[] files = list(remote);
        for (FTPFile file : files) if (filter.accept(file)) fs.add(file);
        return fs.toArray(new FTPFile[fs.size()]);
    }

    /**
     * 
     * @param remote
     * @return
     * @throws FTPConnectionClosedException
     * @throws java.io.IOException
     */
    protected FTPFile[] list(String remote)
            throws SocketException, FTPConnectionClosedException, java.io.IOException {
        if (!remoteDirExist(remote)) {
            lggr.warn("'{}' isn't exist.", remote);
            return new FTPFile[0];
        }
        client.sendCommand("OPTS UTF8", "ON");
        client.setFileType(FTPClient.BINARY_FILE_TYPE);
        FTPFile[] result = client.listFiles(remote);
        return result;
    }

    /**
     * 
     * @param result
     * @throws FTPConnectionClosedException
     * @throws java.io.IOException
     */
    public void list(List<FTPFile> result)
            throws FTPConnectionClosedException, java.io.IOException {
        list(source, new Filter<FTPFile>() {
            @Override
            public boolean accept(FTPFile file) {
                if (file.isDirectory()) return false;
                else if (file.isFile()) return true;
                else return false;
            }
        }, result);
    }

    /**
     * 
     * @param remote
     * @param result
     * @throws FTPConnectionClosedException
     * @throws java.io.IOException
     */
    public void list(
            String remote, List<FTPFile> result)
            throws FTPConnectionClosedException, java.io.IOException {
        list(remote, new Filter<FTPFile>() {
            @Override
            public boolean accept(FTPFile entity) {
                return true;
            }
        }, result);
    }

    /**
     * 
     * @param remote
     * @param filter
     * @param result
     * @throws FTPConnectionClosedException
     * @throws IOException
     */
    public void list(String remote, Filter<FTPFile> filter, List<FTPFile> result)
            throws FTPConnectionClosedException, java.io.IOException {
        // Set UTF-8 for file name.
        client.sendCommand("OPTS UTF8", "ON");
        client.setFileType(FTPClient.BINARY_FILE_TYPE);
        try {
            client.initiateListParsing(remote).getFiles(new FTPFileFilter() {
                @Override
                public boolean accept(FTPFile file) {
                    int ac = FTPFile.GROUP_ACCESS, pe = FTPFile.READ_PERMISSION;
                    if (!file.hasPermission(ac, pe)) return false;
                    if (file.getName().startsWith(".")) return false;
                    //
                    String xrmt = remote.equals("/") ? "" : remote;
                    String xfile = xrmt + "/" + file.getName();
                    //
                    if (file.isDirectory()) {
                        try {
                            lggr.debug("[Dir   ]->{}", xfile);
                            list(xfile, filter, result);
                        }
                        catch (java.io.IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else if (file.isFile()) {
                        lggr.debug("[File  ]--{}", xfile);
                    }
                    else {
                        lggr.debug("[Unknow]--{}", xfile);
                    }
                    //
                    boolean accept = filter.accept(file);
                    if (accept) {
                        file.setName(xfile);
                        result.add(file);
                    }
                    return accept;
                }
            });
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * 
     * @param dir
     * @throws SocketException
     * @throws FTPConnectionClosedException
     * @throws java.io.IOException
     */
    protected void mkdirs(String dir)
            throws SocketException, FTPConnectionClosedException, java.io.IOException {
        if (!client.changeWorkingDirectory(dir)) {
            mkdirs(toRemoteRealPath(Paths.get(dir).getParent().toString()));
            client.makeDirectory(dir);
        }
    }

    @Override
    public boolean dirExist(String remote)
            throws SocketException, FTPConnectionClosedException, IOException {
        return remoteDirExist(remote);
    }

    @Override
    public boolean fileExist(String remote)
            throws SocketException, FTPConnectionClosedException, IOException {
        return remoteFileExist(remote);
    }

}
