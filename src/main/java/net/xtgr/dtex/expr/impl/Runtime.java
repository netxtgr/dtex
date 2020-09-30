/**
 * 
 */

package net.xtgr.dtex.expr.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.xtgr.dtex.expr.prof.Profile;
import net.xtgr.dtex.expr.prof.SvrProf;

/**
 * @author Tiger
 *
 */
public class Runtime {
    Logger lggr;
    public Runtime() throws java.io.IOException {
        lggr = LogManager.getLogger(Runtime.class);
    }

    public void runtime() throws IOException, URISyntaxException, IllegalArgumentException,
            IllegalAccessException {
        Runtime entrance = new Runtime();
        Logger lggr = entrance.lggr;
        lggr.info(
                "{}!@---  Welcome to exchange data on '{}' local with FTPSvr., start up ---@!",
                SvrProf.LF, java.nio.file.Paths.get(".").toRealPath());
        Map<String, Profile.Conf> ccp = new Profile().xparse();
        Iterator<String> it = ccp.keySet().iterator();
        while (it.hasNext()) {
            Profile.Conf conf = ccp.get(it.next());
            if (conf.getScheme().equalsIgnoreCase(SvrProf.SCHEME_FILE)) {
                new UploadTransport(conf).start();
            }
            else if (conf.getScheme().equalsIgnoreCase(SvrProf.SCHEME__FTP)) {
                new DownloadTransport(conf).start();
            }
            else {

            }
        }
    }

}
