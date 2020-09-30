/**
 * 
 */

package net.xtgr.dtex.expr.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Queue;

import org.apache.logging.log4j.Level;

import net.xtgr.dtex.expr.Spread;
import net.xtgr.dtex.expr.Upload;
import net.xtgr.dtex.expr.prof.Profile;
import net.xtgr.dtex.expr.prof.Rule;
import net.xtgr.dtex.expr.prof.Rule.Ruler;
import net.xtgr.dtex.expr.tk.Filter;
import net.xtgr.dtex.expr.tk.Timekeeper;

/**
 * 
 * @author Tiger
 *
 */
public class UploadTransport extends Spread {
    public UploadTransport(Profile.Conf conf)
            throws IOException, IllegalArgumentException, IllegalAccessException {
        super(conf);
    }

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
    class Inspector extends Upload implements net.xtgr.dtex.expr.Inspect {
        boolean display = false;
        public Inspector(Profile.Conf conf) throws IOException {
            super(conf);
        }

        void delegate() throws FileNotFoundException, IOException, InvalidPathException,
                SecurityException {
            Thread se = Thread.currentThread();
            long interval = 60 * 1000;
            new Timekeeper((rt) -> {
                lggr.info("[Keeper]:{}@'{}'.", rt, se);
                display = true;
            }, interval);
            String ms = "[Round:{}] - [{}]";
            long round = 0;
            while (true) try {
                ++round;
                if (lggr.isEnabled(Level.DEBUG)) lggr.debug(ms, round, se);
                else if (lggr.isEnabled(Level.INFO)) {
                    if (display) {
                        display = !display;
                        lggr.info(ms, round, se);
                    }
                }
                synchronized (lock) {
                    lock.wait(618L);
                    if (!locker.isEmpty()) lock.notify();
                    else {
                        lggr.debug("Retrieves uploadable files from <-'{}'.",
                                Paths.get(".").toRealPath().relativize(lock));
                        if (!dirExist(source.toString()))
                            throw new InvalidPathException(source, " isn't directory.");
                        java.util.Queue<String[]> groups = retrieveAvailable();
                        if (groups.size() > 0) {
                            display = true;
                            applies(groups);
                        }
                    }
                }
            }

            catch (FileNotFoundException | InvalidPathException | SecurityException e) {
                throw e;
            }
            catch (InterruptedException e) {
                lggr.warn("[{}] is interrupted.", se);
                Thread.interrupted();
            }
            catch (Exception e) {
                lggr.error(e.getMessage(), e);
                display = true;
            }
        }

        @Override
        public void run() {
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
        public Queue<String[]> retrieveAvailable()
                throws java.io.IOException, java.lang.Exception {
            java.util.Queue<Path> oks = new java.util.ArrayDeque<>();
            java.util.Queue<Path> dts = new java.util.ArrayDeque<>();
            list(source, new Filter<Path>() {
                @Override
                public boolean accept(Path file) {
                    String name = file.toAbsolutePath().toString();
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
            Path[] xdt = dts.toArray(new Path[dts.size()]);
            Validator<?> valid = Validator.getValidator(Path.class);
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
            java.util.Queue<String[]> uploadable = (Queue<String[]>) applier;
            while (!uploadable.isEmpty()) {
                String[] group = uploadable.poll();
                int j = 0;
                String it;
                while (j < group.length) {
                    it = group[j];
                    if (!it.startsWith("/")) group[j] = "/" + it;
                    j++;
                }
                locker.records(group);
            }
        }

        /**
         * 
         * @param ok
         * @param valid
         * @param xdt
         * @return
         */
        String[] mkgroup(Path ok, Validator<?> valid, Path[] xdt) {
            String mixture[], group[];
            try {
                Objects.requireNonNull(ok);
                String local = ok.toString();
                lggr.info("[Make]*[Group]:-[  ok]:-'{}' -\\", local);
                if (Files.isDirectory(ok) || Files.isSymbolicLink(ok)) {
                    throw new RuntimeException(ok + " isn't a file.");
                }
                group = read(local);
                lggr.info("       [Group]:-[size]:-[{}] -\\", group.length);
                if (group.length == 0) return null;
                //
                mixture = new String[group.length + 1];
                String ms = "[L]->[R], '{}'@'{}', [S]:{} .B";
                int i = 0;
                while (i < group.length) {
                    lggr.info("       [Group]:[ mix]:-[{}]'{}'", i + 1, group[i]);
                    if (group[i].isEmpty()) break;
                    @SuppressWarnings("unchecked")
                    String tmpl = ((Validator<Path>) valid).onRule(group[i], xdt);
                    if (Objects.isNull(tmpl) || tmpl.isEmpty()) return null;
                    mixture[i] = mkPaths(tmpl);
                    lggr.info(ms, Ruler.getName(tmpl), source,
                            FILE_SIZE_FORMAT.format(Rule.Ruler.getSize(tmpl)));
                    i++;
                }
                if (i > 0 && i == group.length) {
                    Path name = ok.getFileName();
                    long size = Files.size(ok);
                    mixture[i] = mkPaths(
                            Ruler.template(name + "", "" + size));
                    lggr.info(ms, ok.getFileName(), target,
                            FILE_SIZE_FORMAT.format(Files.size(ok)));
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
                    Paths.get(target).resolve(Rule.Ruler.getName(mixture)).toString()),
                    size = String.valueOf(Rule.Ruler.getSize(mixture));
            return Rule.Ruler.template(path, size);
        }

    }

    /**
     * 
     * @author Tiger
     *
     */
    class Handler extends Upload implements net.xtgr.dtex.expr.Handle {
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
                        lggr.debug("Retrieves uploadable from <-'{}'.",
                                Paths.get(".").toRealPath().relativize(lock));
                        if (!dirExist(source.toString()))
                            throw new InvalidPathException(source, " isn't directory.");
                        String mixture = locker.peek();
                        boolean xnpt = Objects.nonNull(mixture) && !mixture.isEmpty();
                        if (xnpt) {
                            display = false;
                            applies(mixture);
                            locker.poll();
                        }
                    }
                }
            }

            catch (FileNotFoundException | InvalidPathException | SecurityException e) {
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
        public <E> void applies(E applier) throws Exception {
            String mixture = (String) applier, ms;
            lggr.debug("[Poll]:+ '{}'", mixture);
            if (Ruler.getSize(mixture) == 0) {
                lggr.warn("It has nothing in:- '{}'.", mixture);
                return;
            }
            String remote = Ruler.getName(mixture);
            String name = Paths.get(remote).getFileName().toString();
            Path local = Paths.get(source).resolve(name);
            // 1, Upload, if done but didn't moved
            lggr.info("[  Upload]:[Prepare]+ - '{}' -> '{}'", name, target);
            if (upload(local.toString(), remote)) {
                if (Files.size(local) != Ruler.getSize(mixture)) {
                    lggr.warn("Size of it is changed:* - [{}vs{}].B - '{}'.",
                            FILE_SIZE_FORMAT.format(Files.size(local)),
                            FILE_SIZE_FORMAT.format(Ruler.getSize(mixture)), name);
                    throw new IOException("Size of '" + name + "' is changed.");
                }
                lggr.info("[  Upload]:[ Finish]+ - '{}' -> '{}'", name, target);
                // 2, Move,
                if (!Objects.isNull(backup) && !backup.isEmpty()) {
                    String from = local.toString();
                    Path to = Paths.get(backup).resolve(conf.getBackupMode().prefix())
                            .resolve(name);
                    lggr.info("[    Move]:[Prepare]+ - '{}' -> '{}'", from, to);
                    if (rename(from, to.toString())) {
                        lggr.info("[    Move]:[ Finish]+ - '{}' -> '{}'", from, to);
                    }
                    else {
                        ms = "Moved '{" + to + "}' is failed.";
                        throw new IOException(ms);
                    }
                }
                else {
                    lggr.info("[  Delete]:[Prepare]+ - {}", local);
                    if (delete(local.toString())) {
                        lggr.info("[  Delete]:[ Finish]+ - {}", local);
                    }
                    else {
                        ms = "Deleted '{" + local + "}' is failed.";
                        throw new IOException(ms);
                    }
                }
            }
            else {
                String e = "Method 'upload(local, remote)' return 'false'. ";
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
