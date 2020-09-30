/**
 * 
 */

package net.xtgr.dtex.expr;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import net.xtgr.dtex.expr.tk.Timekeeper;

/**
 * @author Tiger
 *
 */
public interface Transport extends Runnable {

    /**
     * 
     * @param <E>
     * @param applier
     * @throws java.lang.Exception
     */
    <E> void applies(E applier) throws java.lang.Exception;

    /**
     * 
     */
    @Override
    public abstract void run();

    /**
     * 
     * @author Tiger
     *
     */
    class Register {
        public void noop() throws java.io.IOException {
            if (client.sendNoOp())
                lggr.debug("[Send]:{}[{}] 'NOOP' with {}.", "+", "successful", self);
            else lggr.warn("[Send]:{}[{}] 'NOOP' with {}.", "*", "failed", self);
        }

        public void count() {
            ++round;
            if (lggr.isEnabled(Level.DEBUG)) lggr.info(mstmpl, round, self);
            else if (lggr.isEnabled(Level.INFO)) {
                if (display) {
                    display = !display;
                    lggr.info(mstmpl, round, self);
                }
            }
        }

        public void flip() {
            display = !display;
        }

        public Register(FTPClient client, long interval) throws IOException {
            this.client = client;
            keeper = new Timekeeper((rt) -> {
                lggr.info("[Keeper]:{}@'{}'.", rt, self);
                display = true;
            }, interval);
        }

        public Register(FTPClient client) throws IOException {
            this(client, 60 * 1000L);
        }

        Thread     self    = Thread.currentThread();
        String     mstmpl  = "[Round:{}] - [{}]";
        long       round   = 0;
        boolean    display = false;
        Timekeeper keeper;
        FTPClient  client;
        Logger     lggr;
    }

}
