/**
 * 
 */

package net.xtgr.dtex.expr.svr;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.xtgr.dtex.expr.prof.SvrProf;
import net.xtgr.dtex.expr.tk.Log4j2Broker;

/**
 * @author Tiger
 *
 */
public class Main {
    public static Logger lggr;
    public Main() {
        lggr = LogManager.getLogger(Main.class);
    }

    /**
     * @param args
     * @throws IOException
     * @throws URISyntaxException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static void start(String[] args) throws IOException, IllegalArgumentException,
            IllegalAccessException, URISyntaxException {
        new Log4j2Broker().initialize(SvrProf.PROFILE_LOGGER);
        new Main();
        lggr.info("Perform it with type of service. it dose start ...");
        new net.xtgr.dtex.expr.impl.Runtime().runtime();

    }

    public static void stop(String[] args) {
        lggr.info("The service dose stop ...");
        System.exit(2);
    }

}
