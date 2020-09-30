/**
 * 
 */

package net.xtgr.dtex.expr.prof;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIBuilder;
import org.ho.yaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.xtgr.dtex.expr.prof.SvrProf.BackupMode;

/**
 * 
 * @author Tiger
 *
 */
public class Profile {
    public Profile() throws java.io.IOException {
        profile = Paths.get(SvrProf.PROFILE_TRANSMIT);
        if (Files.notExists(profile)) {
            Files.createDirectories(profile.toAbsolutePath().getParent());
            Files.createFile(profile);
        }
    }
    volatile Path profile;
    public Path getLock() {
        return profile;
    }

    /**
     * 
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public List<Conf[]> parse() throws URISyntaxException, IOException {
        String[][] guris = (String[][]) Yaml.load(profile.toFile());
        List<Conf[]> confs = new ArrayList<>();
        int i = 0, j = 0;
        while (i < guris.length) {
            Conf conf[] = new Conf[guris[i].length];
            j = 0;
            while (j < guris[i].length) {
                conf[j] = parse(guris[i][j]);
                j++;
            }
            confs.add(conf);
            i++;
        }
        return confs;
    }

    /**
     * 
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public Map<String, Conf> xparse() throws URISyntaxException, IOException {
        @SuppressWarnings("unchecked")
        Map<String, String> guris = (Map<String, String>) Yaml.load(profile.toFile());
        Map<String, Conf> confs = new HashMap<>();
        Iterator<String> it = guris.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next(), value = guris.get(key);
            Conf conf = Profile.parse(value);
            conf.setKey(key);
            confs.put(key, conf);
        }
        return confs;
    }

    /**
     * 
     * @param uri
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public static Conf parse(String uri) throws URISyntaxException, IOException {
        uri = Pattern.compile("[\\\\]{1}").matcher(uri).replaceAll("/");
        java.net.URI xuri = new URIBuilder(uri).build();
        //
        Conf conf = new Conf();
        conf.server = new Conf.Srvc();
        //
        conf.scheme = xuri.getScheme();
        //
        if (conf.scheme.equalsIgnoreCase(SvrProf.SCHEME_FILE)) {
            conf.source = xuri.getPath();
            if (conf.source.startsWith("/")) {
                conf.source = Pattern.compile("/").matcher(conf.source).replaceFirst("");
            }
            //
            String xtarget = URLUtils.extParams(ProfKeys.target.name(), xuri.toURL());
            if (!Objects.isNull(xtarget) && !xtarget.isEmpty()) conf.target = xtarget;
            else conf.target = conf.source;
            if (conf.target.startsWith("/")) {
                conf.target = Pattern.compile("/").matcher(conf.target).replaceFirst("");
            }
            Path _rt = Paths.get("").toAbsolutePath()
                    .relativize(Paths.get(conf.target).toAbsolutePath()),
                    _target = Paths.get(String.valueOf(File.separatorChar)).resolve(_rt);
            conf.target = SvrProf.toRemoteRealPath(_target.toString());
            //
            String isbackup = URLUtils.extParams(ProfKeys.backup.name(), xuri.toURL());
            if (Boolean.valueOf(isbackup)) {
                conf.backup = Paths.get(conf.target).resolve(SvrProf.BACKUP_DEFAULT).toString();
            }
        }
        else if (conf.scheme.equalsIgnoreCase(SvrProf.SCHEME__FTP)) {
            conf.source = xuri.getPath();
            //
            String xtarget = URLUtils.extParams(ProfKeys.target.name(), xuri.toURL());
            if (!Objects.isNull(xtarget) && !xtarget.isEmpty()) conf.target = xtarget;
            else conf.target = conf.source;
            if (conf.target.startsWith("/")) {
                conf.target = Pattern.compile("/").matcher(conf.target).replaceFirst("");
            }
            //
            String isbackup = URLUtils.extParams(ProfKeys.backup.name(), xuri.toURL());
            if (Boolean.valueOf(isbackup)) {
                String _backup = Paths.get(conf.source).resolve(SvrProf.BACKUP_DEFAULT).toString();
                conf.backup = SvrProf.toRemoteRealPath(_backup);
            }
        }
        else {
            String ms = "Unknow scheme:*[" + conf.scheme + "]";
            throw new UnsupportedOperationException(ms);
        }
        //
        String backupMode = URLUtils.extParams(ProfKeys.backupMode.name(), xuri.toURL());
        if (!Objects.isNull(backupMode) && !backupMode.isEmpty()) {
            SvrProf.BackupMode[] modes = SvrProf.BackupMode.values();
            for (SvrProf.BackupMode mode : modes) if (mode.name().equalsIgnoreCase(backupMode)) {
                conf.backupMode = mode;
                break;
            }
        }
        else conf.backupMode = SvrProf.BackupMode.DAILY;
        conf.server.host = xuri.getHost();
        conf.server.port = xuri.getPort() > 0 ? xuri.getPort() : 21;
        String us[] = xuri.getUserInfo().split(":", 2);
        conf.server.user = us[0];
        conf.server.password = us[1];
        return conf;
    }

    /**
     * 
     * @throws java.net.URISyntaxException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void template()
            throws java.net.URISyntaxException, FileNotFoundException, IOException {
        String dw = ruri("/netxtgr/0000/IN", "47.101.71.79", -1, "user", "password",
                ProfKeys.backup.name() + "=true").toString(), up = luri().toString();
        Profile profile = new Profile();
        Yaml.dump(new String[][] {
                new String[] {
                        dw, up
                }, new String[] {
                        up, dw
                }
        }, profile.profile.toFile());
    }

    /**
     * 
     * @throws IOException
     * @throws URISyntaxException
     */
    public static void xtemplate() throws IOException, URISyntaxException {
        String dw = ruri("/netxtgr/0000/IN", "47.101.71.79", -1, "user", "password",
                ProfKeys.backup.name() + "=true").toString(), up = luri().toString();
        Profile profile = new Profile();
        Map<String, String> prof = new TreeMap<>();
        prof.put("xtgr.dw", dw);
        prof.put("xtgr.up", up);
        Yaml.dump(prof, profile.profile.toFile());
    }

    /**
     * 
     * @param path
     * @param host
     * @param port
     * @param user
     * @param password
     * @param queries
     * @return
     * @throws URISyntaxException
     */
    static java.net.URI ruri(String path, String host, int port, String user, String password,
            String... queries) throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(SvrProf.SCHEME__FTP);
        builder.setPath(path);
        builder.setHost(host);
        builder.setPort(port > 0 ? port : 21);
        builder.setUserInfo(user, password);
        for (String query : queries)
            builder.setParameter(query.split("=", 2)[0], query.split("=", 2)[1]);
        return builder.build();
    }

    /**
     * 
     * @param queries
     * @return
     * @throws URISyntaxException
     */
    static java.net.URI luri(String... queries) throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(SvrProf.SCHEME_FILE);
        String xp = Paths.get("").toAbsolutePath() + "";
        if (queries.length == 0)
            builder.setPath(Pattern.compile("\\\\").matcher(xp).replaceAll("/"));
        else if (queries.length == 1) builder.setPath(queries[0]);
        else {
            builder.setPath(queries[0]);
            for (String query : queries)
                builder.setParameter(query.split("=", 2)[0], query.split("=", 2)[1]);
        }
        return builder.build();
    }

    /**
     * 
     * @author Tiger
     *
     */
    public static class Conf {
        @Override
        public String toString() {
            try {
                ObjectMapper xmpr = new ObjectMapper();
                String se = xmpr.writerWithDefaultPrettyPrinter().writeValueAsString(this);
                se = se.replace(server.password, "******");
                return se;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return super.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Conf)) return false;
            Conf other = (Conf) obj;
            return this.label().equals(other.label());
        }

        public String label() {
            return server.label() + scheme + source + target + backup + backupMode.name();
        }

        public void setBackupMode(BackupMode backupMode) {
            this.backupMode = backupMode;
        }

        public BackupMode getBackupMode() {
            return backupMode;
        }

        public void setBackup(String backup) {
            this.backup = backup;
        }

        public String getBackup() {
            return backup;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getTarget() {
            return target;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getSource() {
            return source;
        }

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Srvc getServer() {
            return server;
        }

        public void setServer(Srvc srvc) {
            this.server = srvc;
        }

        String     key, scheme, source, target, backup;
        BackupMode backupMode;
        Srvc       server;
        public Conf() {
        }

        public static class Srvc {
            @Override
            public String toString() {
                try {
                    ObjectMapper xmpr = new ObjectMapper();
                    String se = xmpr.writerWithDefaultPrettyPrinter().writeValueAsString(this);
                    return se;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return super.toString();
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof Srvc)) return false;
                Srvc other = (Srvc) obj;
                return this.label().equals(other.label());

            }

            public String label() {
                return host + port + user + password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getPassword() {
                return password;
            }

            public void setUser(String user) {
                this.user = user;
            }

            public String getUser() {
                return user;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public String getHost() {
                return host;
            }

            public void setPort(int port) {
                this.port = port;
            }

            public int getPort() {
                return port;
            }

            String host, user, password;
            int    port;
            public Srvc() {
            }

        }

    }

}
