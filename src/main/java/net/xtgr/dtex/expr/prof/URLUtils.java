/**
 * 
 */

package net.xtgr.dtex.expr.prof;

/**
 * 
 * @author Tiger
 *
 */
public final class URLUtils {
    public static Builder instance() {
        return new Builder();
    }

    /**
     * 
     * @param key
     * @param cause
     * @return
     */
    public static String get(ProfKeys key, java.net.URL cause) {
        switch (key) {
            case scheme:
                return cause.getProtocol();
            case host:
                return cause.getHost();
            case port:
                return String.valueOf(cause.getPort());
            case source:
                try {
                    return java.net.URLDecoder.decode(cause.getPath(), "utf-8");
                }
                catch (java.io.UnsupportedEncodingException e) {
                    return cause.getPath();
                }
            case user:
            case password:
            case target:
            case backup:
            default:
                return extParams(key.name(), cause);
        }
    }

    /**
     * 
     * @param key
     * @param cause
     * @return
     */
    static String extParams(String key, java.net.URL cause) {
        String query = cause.getQuery();
        if (java.util.Objects.isNull(query) || query.isEmpty()) return null;
        String items[] = query.split("&");
        for (String item : items) {
            String pair[] = item.split("=");
            String xkey = pair[0], value = pair[1], vref;
            if (xkey.equals(key)) {
                try {
                    vref = java.net.URLDecoder.decode(value, "utf-8");
                }
                catch (java.io.UnsupportedEncodingException e) {
                    vref = pair[1];
                }
                return vref;
            }
        }
        return null;
    }

    /**
     * 
     * @author Tiger
     *
     */
    public static class Builder {
        private org.apache.http.client.utils.URIBuilder builder;
        private Builder() {
            builder = new org.apache.http.client.utils.URIBuilder();
        }

        /**
         * 
         * @return
         */
        public java.net.URL toURL() {
            java.net.URL cause = null;
            try {
                cause = builder.build().toURL();
            }
            catch (java.net.MalformedURLException | java.net.URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return cause;
        }

        public java.net.URI toURI() {
            java.net.URI cause = null;
            try {
                cause = builder.build();
            }
            catch (java.net.URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return cause;
        }

        protected Builder setParameter(ProfKeys key, String value) {
            builder.setParameter(key.name(), value);
            return this;
        }

        public Builder setBackup(String backup) {
            return setParameter(ProfKeys.backup, backup);
        }

        public Builder setPassword(String password) {
            return setParameter(ProfKeys.password, password);
        }

        public Builder setUser(String user) {
            return setParameter(ProfKeys.user, user);
        }

        public Builder setTarget(String target) {
            return setParameter(ProfKeys.target, target);
        }

        public Builder setSource(String source) {
            builder.setPath(source);
            return this;
        }

        public Builder setPort(String port) {
            builder.setPort(Integer.parseInt(port));
            return this;
        }

        public Builder setHost(String host) {
            builder.setHost(host);
            return this;
        }

        public Builder setScheme(String scheme) {
            builder.setScheme(scheme);
            return this;
        }

    }

}
