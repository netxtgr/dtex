/**
 * 
 */

package net.xtgr.dtex.expr.prof;

/**
 * 
 * @author Tiger
 *
 */
public interface Rule<E> {
    java.text.NumberFormat FILE_SIZE_FORMAT = new java.text.DecimalFormat(",###");

    /**
     * 
     * @param index
     * @param files
     * @return
     */
    String onRule(String index, E[] files);

    /**
     * 
     * @param mixture
     * @return
     */
    default long getSize(String mixture) {
        return Ruler.getSize(mixture);
    }

    /**
     * 
     * @param mixture
     * @return
     */
    default String getName(String mixture) {
        return Ruler.getName(mixture);
    }

    enum Ruler {
        //
        separator("\b"),
        //
        txt(".txt"),
        //
        ok(".ok");

        /**
         * 
         * @param its
         * @return
         */
        public static String template(String... its) {
            if (its.length == 0) return null;
            StringBuilder tmpr = new StringBuilder();
            for (String it : its) tmpr.append(it).append(separator.value());
            tmpr.deleteCharAt(tmpr.length() - 1);
            return tmpr.toString();
        }

        /**
         * 
         * @param mixture
         * @return
         */
        public static long getSize(String mixture) {
            return Integer.parseInt(split(mixture, 2)[1].trim());
        }

        /**
         * 
         * @param mixture
         * @return
         */
        public static String getName(String mixture) {
            return split(mixture, 2)[0].trim();
        }

        /**
         * 
         * @param mixture
         * @return
         */
        static String[] split(String mixture) {
            return mixture.split(Ruler.separator.value());
        }

        /**
         * 
         * @param mixture
         * @param gps
         * @return
         */
        static String[] split(String mixture, int size) {
            String it[] = new String[size], its[] = split(mixture);
            for (int i = 0; i < size; i++) it[i] = its[i];
            return it;
        }

        /**
         * 
         * @return
         */
        public final String value() {
            return rule;
        }

        /**
         * 
         * @param rule
         */
        Ruler(String rule) {
            this.rule = rule;
        }
        String rule;
    }

    /**
     * 
     * @author Tiger
     *
     */
    enum LockRuler {
        ;
        /**
         * 
         * @param conf
         * @return
         * @throws IllegalArgumentException
         * @throws IllegalAccessException
         */
        public static String makeLockerPath(Profile.Conf conf)
                throws IllegalArgumentException, IllegalAccessException {
            java.util.List<String> fds = new java.util.ArrayList<>();
            java.lang.reflect.Field fd[] = SvrProf.class.getDeclaredFields();
            int i = 0;
            while (i < fd.length) {
                java.lang.reflect.Field dfx = fd[i++];
                int mod = dfx.getModifiers();
                if (java.lang.reflect.Modifier.isPublic(mod)
                        && java.lang.reflect.Modifier.isStatic(mod)
                        && java.lang.reflect.Modifier.isFinal(mod)) {
                    if (!dfx.getName().startsWith("SCHEME_")) continue;
                    fds.add((String) dfx.get(SvrProf.class));
                }
            }
            java.util.Iterator<String> it = fds.iterator();
            while (it.hasNext()) {
                String next = it.next(), name;
                if (next.equalsIgnoreCase(conf.scheme)) {
                    if (next.equalsIgnoreCase(SvrProf.SCHEME_FILE)) name = "up";
                    else if (next.equalsIgnoreCase(SvrProf.SCHEME__FTP)) name = "dw";
                    else {
                        throw new UnsupportedOperationException(next);
                    }
                    String key = conf.key;
                    if (key.contains(name)) name = "";
                    key = java.util.regex.Pattern.compile("\\.").matcher(key).replaceAll("_");
                    // key = key.toUpperCase();
                    name = name.isEmpty() ? String.join("_", key, SvrProf.LOCK)
                            : String.join("_", key, name, SvrProf.LOCK);
                    name = capitalize(name);
                    String path = "";
                    if (next.equalsIgnoreCase(SvrProf.SCHEME_FILE)) path = conf.source;
                    else if (next.equalsIgnoreCase(SvrProf.SCHEME__FTP)) path = conf.target;
                    else {
                    }
                    path += path.isEmpty() ? name : "/" + name;
                    path = java.util.regex.Pattern.compile("[\\\\|/]+").matcher(path)
                            .replaceAll("/");
                    return path;
                }
            }
            return null;
        }

        /**
         * 
         * @param name
         * @return
         */
        public static String capitalize(String name, String... except) {
            java.util.List<String> xs = new java.util.ArrayList<>();
            pieces(name, xs);
            StringBuilder ss = new StringBuilder();
            java.util.Iterator<String> it = xs.iterator();
            boolean isexc = false;
            String x;
            while (it.hasNext()) {
                x = it.next();
                int j = 0;
                while (j < except.length) {
                    if (except[j++].equalsIgnoreCase(x)) {
                        isexc = true;
                        break;
                    }
                }
                if (!isexc) {
                    x = _capitalize(x);
                    isexc = false;
                }

                ss.append(x);
            }
            return ss.toString();
        };

        /**
         * 
         * @param it
         * @return
         */
        static String _capitalize(String it) {
            char x = it.charAt(0);
            while (x > 96 && x < 123) x -= 32;
            return x + it.substring(1);
        }

        /**
         * 
         * @param name
         * @param cs
         */
        static void pieces(String name, java.util.List<String> cs) {
            String regex = "[_|\\.|-]+";
            java.util.regex.Matcher mchr;
            mchr = java.util.regex.Pattern.compile(regex).matcher(name);
            if (mchr.find()) {
                int idx = mchr.start();
                if (idx == 0) {
                    idx += 1;
                }
                cs.add(name.substring(0, idx));
                name = name.substring(idx);
                pieces(name, cs);
            }
            else {
                cs.add(name);
                return;
            }
        };
    }

}
