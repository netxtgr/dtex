
package net.xtgr.dtex.expr.prof;

import java.time.temporal.ChronoField;

/**
 * 
 * @author Tiger
 *
 */
public enum SvrProf {
    //
    WTSC,
    //
    MEDI,
    //
    SHSD,
    //
    ;

    public static final SvrProf WORLD_TRACING                  = WTSC;
    public static final SvrProf MEDIUM                         = MEDI;
    public static final SvrProf SHISEIDO                       = SHSD;

    public static final String  PROFILE_LOGGER                 = "resource/log4j2.xml";
    public static final String  PROFILE_TRANSMIT               = "resource/profile.yaml";

    public static final String  REGEX_PATH_SEPARATOR_CONVERTER = "[\\\\|/]+";
    public static final String  REGEX_FTP_PATH_SEPARATOR       = "/";

    public static final String  LOCK                           = "aims.LOCK";

    public static final String  SCHEME__FTP                    = "ftp";
    public static final String  SCHEME_FILE                    = "file";
    public static final String  BACKUP_DEFAULT                 = "backup";
    // Line Feed
    public static final String  LF                             = "\n";
    // Carriage Return
    public static final String  CR                             = "\r";

    /**
     * 
     * @param path
     * @return
     */
    public static String toRemoteRealPath(String path) {
        return java.util.regex.Pattern.compile(SvrProf.REGEX_PATH_SEPARATOR_CONVERTER)
                .matcher(path).replaceAll(SvrProf.REGEX_FTP_PATH_SEPARATOR);
    }

    /**
     * 
     * @author Tiger
     *
     */
    public enum BackupMode {
        YEARLY, MONTHLY, WEEKLY, DAILY,;

        enum ymwd {
            y(ChronoField.YEAR),
            //
            m(ChronoField.MONTH_OF_YEAR),
            //
            w(ChronoField.ALIGNED_WEEK_OF_YEAR),
            //
            d(ChronoField.DAY_OF_MONTH),;

            static String prefix(ymwd... ymwds) {
                java.time.LocalDate now = java.time.LocalDate.now();
                StringBuilder pfx = new StringBuilder();
                for (ymwd x : ymwds) {
                    pfx.append(it(now, x)).append("-");
                }
                if (pfx.length() > 0) {
                    pfx.deleteCharAt(pfx.length() - 1);
                }
                return pfx.toString();
            }

            static String it(java.time.LocalDate now, ymwd it) {
                int x = now.get(it.it);
                return (x < 10 ? "0" + x : "" + x);
            }

            int value() {
                return java.time.LocalDate.now().get(it);
            }

            ChronoField it;
            ymwd(ChronoField it) {
                this.it = it;
            }

        }

        /**
         * 
         * @return
         */
        public String prefix() {
            switch (this) {
                case WEEKLY: {
                    return ymwd.prefix(ymwd.y, ymwd.w);
                }
                case DAILY: {
                    return ymwd.prefix(ymwd.y, ymwd.m, ymwd.d);
                }
                case MONTHLY: {
                    return ymwd.prefix(ymwd.y, ymwd.m);
                }
                case YEARLY: {
                    return ymwd.prefix(ymwd.y);
                }
            }
            return null;
        }

        /**
         * 
         * @param prefix
         * @param ymwd
         * @return
         */
        public static String prefix(BackupMode prefix, int... ymwd) {
            java.time.LocalDate now = java.time.LocalDate.now();
            int it[] = new int[0];
            switch (prefix) {
                case DAILY: {
                    it = new int[] {
                            now.get(ChronoField.YEAR), now.get(ChronoField.MONTH_OF_YEAR),
                            now.get(ChronoField.DAY_OF_MONTH)
                    };
                    break;
                }
                case WEEKLY: {
                    it = new int[] {
                            now.get(ChronoField.YEAR), now.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
                    };
                    break;
                }
                case MONTHLY: {
                    it = new int[] {
                            now.get(ChronoField.YEAR), now.get(ChronoField.MONTH_OF_YEAR)
                    };
                    break;
                }
                case YEARLY: {
                    it = new int[] {
                            now.get(ChronoField.YEAR)
                    };
                    break;
                }
            }
            StringBuilder pfx = new StringBuilder();
            for (int i : it) pfx.append(i < 10 ? "0" + i : i).append("-");
            pfx.deleteCharAt(pfx.length() - 1);
            return pfx.toString();
        }

        /**
         * 
         * @param prefixes
         * @return
         */
        public static String prefix(int... prefixes) {
            StringBuilder pfxs = new StringBuilder();
            for (int prefix : prefixes) {
                pfxs.append(prefix < 10 ? "0" + prefix : "" + prefix).append("-");
            }
            if (pfxs.length() > 0) pfxs.deleteCharAt(pfxs.length() - 1);
            return pfxs.toString();
        }

        /**
         * 
         * @param prefix
         * @return
         */
        public static String prefix(BackupMode prefix) {
            java.time.LocalDate now = java.time.LocalDate.now();
            StringBuilder pfx = new StringBuilder();
            pfx.append(now.get(ChronoField.YEAR));
            switch (prefix) {
                case YEARLY: {
                    break;
                }
                case MONTHLY: {
                    pfx.append("-");
                    int imon = now.get(ChronoField.MONTH_OF_YEAR);
                    if (imon < 10) pfx.append(0).append(imon);
                    else pfx.append(imon);
                    break;
                }
                case WEEKLY: {
                    pfx.append("-");
                    int iwk = now.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
                    if (iwk < 10) pfx.append(0).append(iwk);
                    else pfx.append(iwk);
                    break;
                }
                case DAILY: {
                    pfx.append("-");
                    int imon = now.get(ChronoField.MONTH_OF_YEAR);
                    if (imon < 10) pfx.append(0).append(imon);
                    else pfx.append(imon);
                    pfx.append("-");
                    int iday = now.get(ChronoField.DAY_OF_MONTH);
                    if (iday < 10) pfx.append(0).append(iday);
                    else pfx.append(iday);
                    break;
                }
            }
            return pfx.toString();
        }
    }

}
