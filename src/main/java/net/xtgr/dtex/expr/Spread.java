
package net.xtgr.dtex.expr;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.xtgr.dtex.expr.prof.Profile;
import net.xtgr.dtex.expr.prof.Rule;
import net.xtgr.dtex.expr.tk.Locker;

public abstract class Spread {
    public final static NumberFormat      FILE_SIZE_FORMAT = new DecimalFormat(",###");
    protected volatile Locker             locker;
    protected Profile.Conf                conf;
    protected volatile java.nio.file.Path lock;
    protected Logger                      lggr;
    public Spread(Profile.Conf conf)
            throws IOException, IllegalArgumentException, IllegalAccessException {
        lggr = LogManager.getLogger(getClass());
        String name = Rule.LockRuler.makeLockerPath(conf);
        locker = new Locker(Paths.get(name));
        lock = locker.getLock();
        this.conf = conf;
    }

    public abstract void start() throws IOException;

}
