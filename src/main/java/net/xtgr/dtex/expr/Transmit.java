/**
 * 
 */

package net.xtgr.dtex.expr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.xtgr.dtex.expr.prof.Profile;
import net.xtgr.dtex.expr.prof.Rule;
import net.xtgr.dtex.expr.prof.SvrProf;
import net.xtgr.dtex.expr.tk.Filter;

/**
 * 
 * @author Tiger
 *
 * @param <E>
 */
public abstract class Transmit<E> {
    protected String    host, user, password;
    protected String    source, target, backup;
    protected int       port;
    protected FTPClient client;
    protected Logger    lggr;
    protected Transmit(Profile.Conf conf) throws IOException {
        lggr = LogManager.getLogger(Transmit.class);
        //
        lggr.debug("Configure: {}", conf);
        source = conf.getSource();
        target = conf.getTarget();
        backup = conf.getBackup();
        //
        Profile.Conf.Srvc srvc = conf.getServer();
        host = srvc.getHost();
        port = srvc.getPort();
        user = srvc.getUser();
        password = srvc.getPassword();
        //
        lggr.info("[Client]: is building for {}.", this.getClass());
        client = new FTPClient();
        __configure(client);
        tipconf();
        //
    }

    /**
     * 
     * @param remote
     * @param local
     * @return
     * @throws java.net.SocketException
     * @throws org.apache.commons.net.ftp.FTPConnectionClosedException
     * @throws org.apache.commons.net.io.CopyStreamException
     * @throws java.io.IOException
     */
    public abstract boolean upload(String local, String remote) throws java.net.SocketException,
            org.apache.commons.net.ftp.FTPConnectionClosedException,
            org.apache.commons.net.io.CopyStreamException, java.io.IOException;

    /**
     * 
     * @param remote
     * @param local
     * @return
     * @throws java.net.SocketException
     * @throws org.apache.commons.net.ftp.FTPConnectionClosedException
     * @throws org.apache.commons.net.io.CopyStreamException
     * @throws java.io.IOException
     */
    public abstract boolean download(String remote, String local) throws java.net.SocketException,
            org.apache.commons.net.ftp.FTPConnectionClosedException,
            org.apache.commons.net.io.CopyStreamException, java.io.IOException;

    /**
     * 
     * @param file
     * @return
     * @throws java.net.SocketException
     * @throws java.io.IOException
     */
    public abstract boolean delete(String file)
            throws java.net.SocketException, java.io.IOException;

    /**
     * 
     * @param from
     * @param to
     * @return
     * @throws java.net.SocketException
     * @throws java.io.IOException
     */
    public abstract boolean rename(String from, String to)
            throws java.net.SocketException, java.io.IOException;

    /**
     * 
     * @param file
     * @return
     * @throws java.net.SocketException
     * @throws java.io.IOException
     */
    public abstract String[] read(String file)
            throws java.net.SocketException, java.io.IOException;

    /**
     * 
     * @param file
     * @param filter
     * @return
     * @throws java.net.SocketException
     * @throws java.io.IOException
     */
    public abstract E[] list(String file, Filter<E> filter)
            throws java.net.SocketException, java.io.IOException;

    /**
     * 
     * @param dir
     * @return
     * @throws java.net.SocketException
     * @throws java.io.IOException
     */
    public abstract boolean dirExist(String dir)
            throws java.net.SocketException, java.io.IOException;

    /**
     * 
     * @param dir
     * @return
     * @throws IOException
     */
    protected boolean remoteDirExist(
            String dir) throws SocketException, FTPConnectionClosedException, IOException {
        return client.changeWorkingDirectory(dir);
    }

    /**
     * 
     * @param dir
     * @return
     */
    protected boolean localDirExist(String dir)
            throws InvalidPathException, SecurityException, FileNotFoundException {
        Path path = Paths.get(dir);
        boolean is = Files.exists(path);
        if (!is) {
            lggr.error("The file '{}' does not exist.", path);
            throw new FileNotFoundException("" + path);
        }
        else {
            is = Files.isDirectory(path);
            if (!is) {
                lggr.error("The file '{}' isn't a directory.", path);
                return false;
            }
        }
        return true;
    }

    /**
     * 
     * @param file
     * @return
     * @throws java.net.SocketException
     * @throws java.io.IOException
     */
    public abstract boolean fileExist(String file)
            throws java.net.SocketException, java.io.IOException;

    /**
     * 
     * @param file
     * @return
     * @throws IOException
     */
    protected boolean remoteFileExist(
            String file)
            throws SocketException, FTPConnectionClosedException, java.io.IOException {
        boolean exist = client.listFiles(file).length > 0;
        if (!exist) lggr.error("The file '{}' does not exist.", file);
        return exist;
    }

    /**
     * 
     * @param file
     * @return
     */
    protected boolean localFileExist(String file) throws IOException {
        Path path = Paths.get(file);
        boolean is = Files.exists(path);
        if (!is) {
            lggr.error("The file '{}' does not exist.", file);
            return false;
        }
        else {
            is = !Files.isDirectory(path) && !Files.isSymbolicLink(path);
            if (!is) {
                lggr.error("The file '{}' isn't a file.", path);
                return false;
            }
        }
        return true;
    }

    /**
     * 
     * @param path
     * @return
     */
    public static String toRemoteRealPath(String path) {
        return SvrProf.toRemoteRealPath(path);
    }

    /**
     * 
     * @return
     * @throws java.net.UnknownHostException
     * @throws java.net.SocketException
     * @throws org.apache.commons.net.ftp.FTPConnectionClosedException
     * @throws java.io.IOException
     */
    @Deprecated
    protected boolean open() throws UnknownHostException, SocketException,
            FTPConnectionClosedException, IOException {
        String ms = "@@!-- [{}], Prepare to connect [{}] --!@@";
        lggr.debug(ms, Thread.currentThread(), host);
        boolean opened = true;
        // 1, Connect to remote.
        client.connect(host, port);
        if (FTPReply.isPositiveCompletion(client.getReplyCode()))
            ms = "[{}] is connected on [{}].";
        else {
            ms = "Can't connect server on {}:{}";
            opened = false;
        }
        lggr.debug(ms, host, port);
        /**
         * Usually, we use FTP transfer server in the Intranet.You need to set
         * both the server and client to be in passive mode.If the setting
         * fails, it will not work properly.
         */
        // 2, Set passive mode open random port.
        client.enterLocalPassiveMode();
        lggr.debug("Set passive mode open random port.");
        // 3, Login remote.
        if (client.login(user, password)) ms = "[{}] has logined.";
        else {
            ms = "User:'{}' and password maybe mistake.";
            opened = false;
        }
        lggr.debug(ms, user);
        // 4, Set transfer type.
        client.setFileType(FTPClient.BINARY_FILE_TYPE);
        lggr.debug("Set transfer file with binary type.");
        return opened;
    }

    /**
     * 
     * @return
     * @throws java.net.SocketException
     * @throws org.apache.commons.net.ftp.FTPConnectionClosedException
     * @throws java.io.IOException
     */
    @Deprecated
    protected void close()
            throws java.net.SocketException, FTPConnectionClosedException, IOException {
        String ms;
        // 5, Logout remote.
        if (client.logout()) ms = "[{}] has logouted.";
        else ms = "User:'{}' can't logout.";
        lggr.debug(ms, user);
        // 6, Close connection.
        client.disconnect();
        if (FTPReply.isPositiveCompletion(client.getReplyCode()))
            ms = "[{}] connection has been close on [{}].";
        else ms = "Can't close connection on {}:{}";
        lggr.debug(ms, host, port);
    }

    /**
     * 
     */
    protected void tipconf() {
        String m = "[Control Encoding]: [{}], The file name with multi-byte character";
        m += " representations can be specified.";
        lggr.info(m, client.getControlEncoding());
        lggr.info("[Default timeout]: is set [{}]mS.",
                Rule.FILE_SIZE_FORMAT.format(client.getDefaultTimeout()));
        lggr.info("[Control keepalive reply timeout]: is set [{}]mS.",
                Rule.FILE_SIZE_FORMAT.format(client.getControlKeepAliveReplyTimeout()));
        lggr.info("[Control keepalive timeout]: is set [{}]mS.",
                Rule.FILE_SIZE_FORMAT.format(client.getControlKeepAliveTimeout()));
        lggr.info("[Connection timeout]: is set [{}]mS.",
                Rule.FILE_SIZE_FORMAT.format(client.getConnectTimeout()));
        lggr.info("[Data timeout]: maybe set [{}]mS.", "???");
    }

    /**
     * 
     * @return
     * @throws SocketException
     * @throws IOException
     */
    protected boolean __open() throws SocketException, IOException {
        String ms = "@@!-- [{}], Prepare to connect [{}] --!@@";
        lggr.debug(ms, Thread.currentThread(), host);
        client.connect(host, port);
        if (FTPReply.isPositiveCompletion(client.getReplyCode())) {
            lggr.info("[Connection]: has been opened.");
            /**
             * Usually, we use FTP transfer server in the Intranet.You need to
             * set both the server and client to be in passive mode.If the
             * setting fails, it will not work properly.
             * 
             * Set passive mode open random port.
             */
            client.enterLocalPassiveMode();
            client.setFileType(FTPClient.BINARY_FILE_TYPE);
            client.setSoTimeout(1 * 60 * 1000);
            ms = "[Download] set connection open timeout:'{}'ms.";
            lggr.debug(ms, client.getSoTimeout());
            lggr.info("[Login]: [{}@{}:{}].", user, host, port);
            if (client.login(user, password)) lggr.info("[User]: has logined.");
            else {
                throw new IOException();
            }
        }
        else throw new IOException();
        return true;
    }

    /**
     * 
     * @return
     * @throws SocketException
     * @throws IOException
     */
    protected boolean __reopen() throws SocketException, IOException {
        String ms = "@@!-- [{}], Re-open connection [{}] --!@@";
        lggr.debug(ms, Thread.currentThread(), host);
        if (client.isConnected()) {
            lggr.info("[Close]: connection with {}.", Thread.currentThread());
            client.disconnect();
            lggr.info("[Connection]: has been closed.");
        }
        lggr.info("[Open]: connection with {}.", Thread.currentThread());
        return __open();
    }

    /**
     * 
     * @param client
     * @throws java.io.IOException
     */
    protected void __configure(FTPClient client) throws java.io.IOException {
        lggr.info("[Client]: is configured for {}.", this.getClass());
        int unit = 1 * 60 * 1000;
        client.setControlEncoding("UTF-8");
        client.setDefaultTimeout(5 * unit);
        client.setControlKeepAliveReplyTimeout(1 * unit);
        client.setControlKeepAliveTimeout(1 * unit);
        client.setConnectTimeout(3 * unit);
        client.setDataTimeout(1 * unit);
    }

    /**
     * 
     * @param client
     * @throws java.io.IOException
     */
    protected void __reconfigure(FTPClient client) throws java.io.IOException {
        lggr.info("[Client]: is reconfigured for {}.", this.getClass());
        client.reinitialize();
        __configure(client);
    }

    /**
     * 
     * @return
     */
    public String getSource() {
        return source;
    }

    /**
     * 
     * @param source
     */
    public void changeSource(String source) {
        this.source = source;
    }

    /**
     * 
     * @return
     */
    public String getTarget() {
        return target;
    }

    /**
     * 
     * @param target
     */
    public void changeTarget(String target) {
        this.target = target;
    }

    /**
     * 
     * @return
     */
    public String getBackup() {
        return backup;
    }

    /**
     * 
     * @param backup
     */
    public void changeBackup(String backup) {
        this.backup = backup;
    }

}
