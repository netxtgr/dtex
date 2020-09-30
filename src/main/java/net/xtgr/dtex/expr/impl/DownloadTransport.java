/**
 * 
 */

package net.xtgr.dtex.expr.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Queue;

import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.logging.log4j.Level;

import net.xtgr.dtex.expr.Download;
import net.xtgr.dtex.expr.Spread;
import net.xtgr.dtex.expr.prof.Profile;
import net.xtgr.dtex.expr.prof.Rule;
import net.xtgr.dtex.expr.prof.Rule.Ruler;
import net.xtgr.dtex.expr.prof.SvrProf;
import net.xtgr.dtex.expr.tk.Filter;
import net.xtgr.dtex.expr.tk.Timekeeper;

/**
 * 
 * @author Tiger
 *
 */
public class DownloadTransport extends Spread {
    public DownloadTransport(Profile.Conf conf)
            throws IOException, IllegalArgumentException, IllegalAccessException {
        super(conf);
    }

    @Override
    public void start() throws IOException {
        String key = conf.getKey();
        new Thread(new Inspector(conf), key + "@Inspector").start();
        new Thread(new Handler(conf), key + "@Handler").start();
    }

    /**
     * 
     * @author Tiger
     *
     */
    class Inspector extends Download implements net.xtgr.dtex.expr.Inspect {
        boolean display = false;
        public Inspector(Profile.Conf conf) throws IOException {
            super(conf);
        }

        void delegate() throws java.net.UnknownHostException, java.io.FileNotFoundException,
                java.net.SocketException, java.io.IOException {
            Thread se = Thread.currentThread();
            long interval = 60 * 1000;
            new Timekeeper((rt) -> {
                lggr.info("[Keeper]:{}@'{}'.", rt, se);
                display = true;
            }, interval);
            String ms = "[Round:{}] - [{}]";
            long round = 0;
            __open();
            while (true) try {
                ++round;
                if (lggr.isEnabled(Level.DEBUG)) lggr.debug(ms, round, se);
                else if (lggr.isEnabled(Level.INFO)) {
                    if (display) {
                        display = !display;
                        lggr.info(ms, round, se);
                    }
                }
                if (client.sendNoOp())
                    lggr.debug("[Send]:{}[{}] 'NOOP' with {}.", "+", "successful", se);
                else lggr.warn("[Send]:{}[{}] 'NOOP' with {}.", "*", "failed", se);
                synchronized (lock) {
                    lock.wait(618L);
                    if (!locker.isEmpty()) lock.notify();
                    else {
                        lggr.info("Retrieves downloadable from <-'{}'.",
                                Paths.get(".").toRealPath().relativize(lock));
                        if (!dirExist(source)) throw new IOException(source + " isn't exist.");
                        java.util.Queue<String[]> groups = retrieveAvailable();
                        if (groups.size() > 0) {
                            display = true;
                            applies(groups);
                        }
                    }
                }
            }

            catch (java.io.FileNotFoundException e) {
                throw e;
            }
            catch (InterruptedException e) {
                lggr.warn("{} is interrupted.", se);
                Thread.interrupted();
            }
            catch (IOException e) {
                lggr.error(e.getMessage(), e);
                display = true;
                try {
                    __reopen();
                }
                catch (IOException cause) {
                    lggr.error(e.getMessage(), e);
                }
            }
            catch (Exception e) {
                lggr.error(e.getMessage(), e);
                display = true;
            }
        }

        @Override
        public void run() {
            tipconf();
            try {
                delegate();
            }
            catch (Exception e) {
                lggr.error(e.getMessage(), e);
                String m = "[{}], System exit due to the thread exception";
                lggr.error(m, Thread.currentThread());
                System.exit(-1);
            }
        }

        /**
         * 
         * @return
         * @throws java.io.IOException
         * @throws java.lang.Exception
         */
        @Override
        public java.util.Queue<String[]> retrieveAvailable()
                throws java.io.IOException, java.lang.Exception {
            java.util.Queue<FTPFile> oks = new java.util.ArrayDeque<>();
            java.util.Queue<FTPFile> dts = new java.util.ArrayDeque<>();
            list(source, new Filter<FTPFile>() {
                @Override
                public boolean accept(FTPFile file) {
                    if (!file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION)
                            || !file.isFile())
                        return false;
                    file.setName(toRealPath(file));
                    String name = file.getName();
                    if (Validator.verify(name, Rule.Ruler.ok)) oks.add(file);
                    else if (Validator.verify(name, Rule.Ruler.txt)) dts.add(file);
                    else {
                    }
                    return true;
                }
            });
            java.util.Queue<String[]> groups;
            groups = new java.util.LinkedList<String[]>();
            if (oks.isEmpty() || dts.isEmpty()) return groups;
            //
            FTPFile[] xdt = dts.toArray(new FTPFile[dts.size()]);
            Validator<?> valid = Validator.getValidator(FTPFile.class);
            int gsize = 0;
            String[] group;
            while (!oks.isEmpty()) {
                lggr.info("[Group]:[{}]+\\-", ++gsize);
                group = mkgroup(oks.poll(), valid, xdt);
                if (Objects.nonNull(group)) {
                    lggr.info("      \\-[{}]+ items.", group.length);
                    groups.offer(group);
                }
            }
            return groups;
        }

        @Override
        public <E> void applies(E applier) throws Exception {
            @SuppressWarnings("unchecked")
            java.util.Queue<String[]> downloadable = (Queue<String[]>) applier;
            while (!downloadable.isEmpty()) {
                String[] dwbl = downloadable.poll();
                if (Objects.nonNull(dwbl)) locker.records(dwbl);
            }
        }

        /**
         * 
         * @param ok
         * @param valid
         * @param xdt
         * @return
         */
        String[] mkgroup(FTPFile ok, Validator<?> valid, FTPFile[] xdt) {
            String mixture[], group[];
            try {
                Objects.requireNonNull(ok);
                String remote = ok.getName();
                lggr.info("[Make]*[Group]:-[  ok]:-'{}' -\\", remote);
                if (!ok.isFile()) {
                    throw new RuntimeException(remote + " isn't a file.");
                }
                group = read(remote);
                lggr.info("       [Group]:-[size]:-[{}] -\\", group.length);
                if (group.length == 0) return null;
                //
                mixture = new String[group.length + 1];
                String ms = "[R]->[L], '{}'@'{}', [S]:{} .B";
                int i = 0;
                while (i < group.length) {
                    lggr.info("       [Group]:[ mix]:-[{}]'{}'", i + 1, group[i]);
                    if (group[i].isEmpty()) break;
                    @SuppressWarnings("unchecked")
                    String tmpl = ((Validator<FTPFile>) valid).onRule(group[i], xdt);
                    if (Objects.isNull(tmpl) || tmpl.isEmpty()) return null;;
                    mixture[i] = mkPaths(tmpl);
                    lggr.info(ms, Ruler.getName(tmpl), target,
                            FILE_SIZE_FORMAT.format(Ruler.getSize(mixture[i])));
                    i++;
                }
                if (i > 0 && i == group.length) {
                    String name = ok.getName();
                    long size = ok.getSize();
                    mixture[i] = mkPaths(Ruler.template(name, "" + size));
                    lggr.info(ms, Paths.get(name).getFileName(), target,
                            FILE_SIZE_FORMAT.format(size));
                    return mixture;
                }
            }
            catch (Exception e) {
                lggr.error(e.getMessage(), e);
            }
            return null;
        }

        /**
         * 
         * @param mixture
         * @return
         */
        String mkPaths(String mixture) {
            String path = toRemoteRealPath(
                    Paths.get(source).resolve(Rule.Ruler.getName(mixture)).toString()),
                    size = String.valueOf(Rule.Ruler.getSize(mixture));
            return Rule.Ruler.template(path, size);
        }

        /**
         * 
         * @param file
         * @return
         */
        protected String toRealPath(FTPFile file) {
            return SvrProf.toRemoteRealPath(
                    java.nio.file.Paths.get(source).resolve(file.getName()).toString());
        }

    }

    class Handler extends Download implements net.xtgr.dtex.expr.Handle {
        boolean display = false;
        public Handler(Profile.Conf conf) throws IOException {
            super(conf);
        }

        void delegate() throws java.net.UnknownHostException, java.io.FileNotFoundException,
                java.net.SocketException, java.io.IOException {
            Thread se = Thread.currentThread();
            long interval = 60 * 1000;
            new Timekeeper((rt) -> {
                lggr.info("[Keeper]:{}@'{}'.", rt, se);
                display = true;
            }, interval);
            String ms = "[Round:{}] - [{}]";
            long round = 0;
            __open();
            while (true) try {
                ++round;
                if (lggr.isEnabled(Level.DEBUG)) lggr.debug(ms, round, se);
                else if (lggr.isEnabled(Level.INFO)) {
                    if (display) {
                        display = !display;
                        lggr.info(ms, round, se);
                    }
                }
                if (client.sendNoOp())
                    lggr.debug("[Send]:{}[{}] 'NOOP' with {}.", "+", "successful", se);
                else lggr.warn("[Send]:{}[{}] 'NOOP' with {}.", "*", "failed", se);
                synchronized (lock) {
                    lock.wait(618L);
                    if (locker.isEmpty()) lock.notify();
                    else {
                        lggr.info("Retrieves downloadable from <-'{}'.",
                                Paths.get(".").toRealPath().relativize(lock));
                        if (!dirExist(source)) throw new IOException(source + " isn't exist.");
                        String mixture = locker.peek();
                        boolean xnpt = !Objects.isNull(mixture) && !mixture.isEmpty();
                        if (xnpt) {
                            display = true;
                            applies(mixture);
                            locker.poll();
                        }
                    }
                }
            }

            catch (java.io.FileNotFoundException e) {
                throw e;
            }
            catch (InterruptedException e) {
                lggr.warn("{} is interrupted.", se);
                Thread.interrupted();
            }
            catch (IOException e) {
                lggr.error(e.getMessage(), e);
                display = true;
                try {
                    __reopen();
                }
                catch (IOException ie) {
                    lggr.error(ie.getMessage(), ie);
                }
            }
            catch (Exception e) {
                lggr.error(e.getMessage(), e);
            }
        }

        @Override
        public void run() {
            tipconf();
            try {
                delegate();
            }
            catch (IOException e) {
                lggr.error(e.getMessage(), e);
                String m = "[{}], System exit due to the thread exception";
                lggr.error(m, Thread.currentThread());
                System.exit(-1);
            }
        }

        @Override
        public <E> void applies(E applier) throws java.net.SocketException,
                FTPConnectionClosedException, CopyStreamException, java.io.IOException {
            String mixture = (String) applier, ms;
            lggr.debug("[Poll]:+ '{}'", mixture);
            if (Ruler.getSize(mixture) == 0) {
                lggr.warn("It has nothing in:- '{}'.", mixture);
                return;
            }
            String remote = Ruler.getName(mixture);
            String name = Paths.get(remote).getFileName().toString();
            Path local = Paths.get(target).resolve(name);
            // 1, Download, if done but didn't moved
            lggr.info("[Download]:[Prepare]+ - '{}' -> '{}'", name, target);
            if (download(remote, local.toString())) {
                if (Files.size(local) != Ruler.getSize(mixture)) {
                    lggr.warn("Size of it is changed:* - [{}vs{}].B - '{}'.",
                            FILE_SIZE_FORMAT.format(Files.size(local)),
                            FILE_SIZE_FORMAT.format(Ruler.getSize(mixture)), name);
                    throw new IOException("Size of '" + name + "' is changed.");
                }
                lggr.info("[Download]:[ Finish]+ - '{}' -> '{}'", name, target);
                // 2, Move,
                if (!Objects.isNull(backup) && !backup.isEmpty()) {
                    String from = remote, _to;
                    Path to = Paths.get(backup).resolve(conf.getBackupMode().prefix())
                            .resolve(name);
                    _to = toRemoteRealPath(to.toString());
                    lggr.info("[    Move]:[Prepare]+ - '{}' -> '{}'", from, _to);
                    if (rename(from, _to)) {
                        lggr.info("[    Move]:[ Finish]+ - '{}' -> '{}'", from, _to);
                    }
                    else {
                        ms = "Moved '" + to + "' is failed.";
                        throw new IOException(ms);
                    }
                }
                else {
                    lggr.info("[  Delete]:[Prepare]+ - {}", remote);
                    if (delete(remote)) {
                        lggr.info("[  Delete]:[ Finish]+ - {}", remote);
                    }
                    else {
                        ms = "Deleted '" + remote + "' is failed.";
                        throw new IOException(ms);
                    }
                }
            }
            else {
                String e = "Method 'download(remote, local)' return 'false'. ";
                String dir = Paths.get(remote).getParent().toString();
                dir = toRemoteRealPath(dir);
                if (!remoteDirExist(dir)) {
                    e += "Maybe remote directory '" + dir + "' isn't exist";
                }
                throw new IllegalArgumentException(e);
            }
        }

    }

}
