/**
 * 
 */

package net.xtgr.dtex.expr.tk;

/**
 * 
 * @author Tiger
 *
 */
public class Locker {
    volatile java.nio.file.Path lock;
    public Locker(java.nio.file.Path lock) throws java.io.IOException {
        this.lock = buildLock(lock);
    }

    /**
     * 
     * @param lock
     * @return
     * @throws java.io.IOException
     */
    public static java.nio.file.Path buildLock(java.nio.file.Path lock)
            throws java.io.IOException {
        lock = lock.toAbsolutePath();
        if (java.nio.file.Files.notExists(lock)) {
            java.nio.file.Path lp = lock.getParent();
            if (java.nio.file.Files.notExists(lp)) {
                java.nio.file.Files.createDirectories(lp);
            }
            java.nio.file.Files.createFile(lock);
        }
        return lock;
    }

    /**
     * 
     * @param its
     * @throws java.io.IOException
     */
    public synchronized void records(String... its) throws java.io.IOException {
        if (its.length == 0) return;
        java.util.Deque<String> xels = elements();
        java.util.Deque<String> xits = new java.util.LinkedList<>();
        for (String it : its) xits.offerLast(it);
        java.util.Iterator<String> xxit = xits.iterator();
        String xit, eit;
        while (xxit.hasNext()) {
            xit = xxit.next();
            java.util.Iterator<String> eeit = xels.iterator();
            while (eeit.hasNext()) {
                eit = eeit.next();
                if (eit.equals(xit)) {
                    xxit.remove();
                    break;
                }
            }
        }
        xits.addAll(xels);
        //
        try (java.io.BufferedWriter bw = java.nio.file.Files.newBufferedWriter(lock);) {
            String xt;
            while (!xits.isEmpty()) {
                xt = xits.pollFirst();
                if (java.util.Objects.isNull(xt) || xt.isEmpty()) continue;
                bw.write(xt);
                bw.newLine();
                bw.flush();
            }
        }
    }

    /**
     * 
     * @return
     * @throws java.io.IOException
     */
    public synchronized String poll() throws java.io.IOException {
        java.util.Deque<String> dque = elements();
        String result = null;
        if (!dque.isEmpty()) result = dque.pollFirst();
        try (java.io.BufferedWriter bw = java.nio.file.Files.newBufferedWriter(lock);) {
            while (!dque.isEmpty()) {
                bw.write(dque.pollFirst());
                bw.newLine();
                bw.flush();
            }
        }
        return result;
    }

    /**
     * 
     * @return
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public synchronized String peek() throws java.io.FileNotFoundException, java.io.IOException {
        java.util.Deque<String> dque = elements();
        String result = null;
        if (!dque.isEmpty()) result = dque.peekFirst();
        return result;
    }

    /**
     * 
     * @param e
     * @throws java.io.IOException
     */
    public synchronized void offer(String e) throws java.io.IOException {
        java.util.Objects.requireNonNull(e);
        if (e.isEmpty()) throw new IllegalArgumentException("Argument is empty.");
        //
        java.util.Deque<String> dque = elements();
        java.util.Iterator<String> it = dque.iterator();
        while (it.hasNext()) if (it.next().equals(e)) return;
        //
        dque.offerLast(e);
        try (java.io.BufferedWriter bw = java.nio.file.Files.newBufferedWriter(lock);) {
            while (!dque.isEmpty()) {
                bw.write(dque.pollFirst());
                bw.newLine();
                bw.flush();
            }
        }
    }

    /**
     * 
     * @param e
     * @return
     * @throws java.io.IOException
     */
    public synchronized boolean contains(String e) throws java.io.IOException {
        java.util.Deque<String> dque = elements();
        while (!dque.isEmpty()) {
            if (dque.pollFirst().equals(e)) return true;
        }
        return false;
    }

    /**
     * 
     * @return
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public synchronized boolean isEmpty()
            throws java.io.FileNotFoundException, java.io.IOException {
        try (java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(lock);) {
            String line;
            while (java.util.Objects.nonNull(line = br.readLine())) {
                if (!line.isEmpty()) return false;
            }
            return true;
        }
    }

    /**
     * 
     * @return
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public synchronized java.util.Deque<String> elements()
            throws java.io.FileNotFoundException, java.io.IOException {
        java.util.Deque<String> dque = new java.util.LinkedList<>();
        String line;
        try (java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(lock);) {
            while (java.util.Objects.nonNull(line = br.readLine())) {
                if (!line.isEmpty()) dque.addLast(line);
            }
            return dque;
        }
        catch (java.io.IOException e) {
            dque.clear();
            throw e;
        }
    }

    /**
     * 
     * @return
     */
    public synchronized java.nio.file.Path getLock() {
        return lock;
    }

}
