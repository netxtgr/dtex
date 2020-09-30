/**
 * 
 */

package net.xtgr.dtex.expr.tk;

/**
 * 
 * @author Tiger
 *
 */
public class Log4j2Broker {
    java.nio.file.Path profile;
    public Log4j2Broker(java.nio.file.Path profile) {
        this.profile = profile;
    }

    public Log4j2Broker(String profile) {
        this.profile = java.nio.file.Paths.get(profile);
    }

    public Log4j2Broker() {
        profile = java.nio.file.Paths.get(net.xtgr.dtex.expr.prof.SvrProf.PROFILE_LOGGER);
    }

    /**
     * 
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public void initialize() throws java.io.FileNotFoundException, java.io.IOException {
        java.util.Objects.requireNonNull(profile);
        java.io.File file = profile.toFile();
        try (java.io.FileInputStream in = new java.io.FileInputStream(file);) {
            org.apache.logging.log4j.core.config.ConfigurationSource sc;
            sc = new org.apache.logging.log4j.core.config.ConfigurationSource(in, file);
            org.apache.logging.log4j.core.config.Configurator.initialize(null, sc);
        }
    }

    /**
     * 
     * @param profile
     * @throws java.io.IOException
     */
    public void initialize(String profile) throws java.io.IOException {
        if (java.util.Objects.isNull(profile) || profile.isEmpty()) {
            profile = net.xtgr.dtex.expr.prof.SvrProf.PROFILE_LOGGER;
        }
        java.io.File file = java.nio.file.Paths.get(profile).toFile();
        try (java.io.FileInputStream in = new java.io.FileInputStream(file);) {
            org.apache.logging.log4j.core.config.ConfigurationSource sc;
            sc = new org.apache.logging.log4j.core.config.ConfigurationSource(in, file);
            org.apache.logging.log4j.core.config.Configurator.initialize(null, sc);
        }
    }

}
