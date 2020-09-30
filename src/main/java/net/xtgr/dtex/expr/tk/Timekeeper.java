
package net.xtgr.dtex.expr.tk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author Tiger
 *
 */
public class Timekeeper {
    static DecimalFormat format = new DecimalFormat(",###.####");
    Logger               lggr;
    LocalDateTime        clock;
    long                 theMoment, interval;

    public Timekeeper(Consumer<String> cb, long interval) throws IOException {
        this(interval);
        String tid = "Alarm@" + cb.getClass().getName();
        new Thread(new Alarm(cb), tid).start();
    }

    public Timekeeper(long interval) throws IOException {
        this();
        this.interval = interval;
    }

    public Timekeeper() throws FileNotFoundException, IOException {
        lggr = LogManager.getLogger(Timekeeper.class);
        clock();
        clock = LocalDateTime.now();
        String _clock = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(clock);
        lggr.info("Take a clock at '{}'.", _clock);
    }

    /**
     * 
     * @param cb
     */
    public void alarm(Consumer<String> cb) {
        long pass = System.currentTimeMillis() - theMoment;
        String guarding = format.format(BigDecimal.valueOf(pass).divide(BigDecimal.valueOf(1000),
                2, RoundingMode.HALF_EVEN));
        int rounded = (int) (pass / interval);
        StringBuilder sb = new StringBuilder();
        sb.append("[Guard:'").append(guarding).append("'S.]");
        sb.append("&").append("[Round:'").append(rounded).append("'R.]");
        cb.accept(sb.toString());
    }

    class Alarm implements Runnable {
        @Override
        public synchronized void run() {
            for (;;) try {
                wait(interval);
                alarm(cb);
            }
            catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
        Consumer<String> cb;
        Alarm(Consumer<String> cb) {
            this.cb = cb;
        }
    }

    /**
     * 
     * @param interval
     */
    public void changeInterval(long interval) {
        this.interval = interval;
    }

    /**
     * 
     */
    public void clock() {
        theMoment = System.currentTimeMillis();
    }

}
