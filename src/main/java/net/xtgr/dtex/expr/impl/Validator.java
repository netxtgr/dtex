/**
 * 
 */

package net.xtgr.dtex.expr.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.xtgr.dtex.expr.Spread;
import net.xtgr.dtex.expr.prof.Rule;

/**
 * 
 * @author Tiger
 *
 * @param <E>
 */
public abstract class Validator<E> implements Rule<E> {
    protected Logger        lggr = LogManager.getLogger(Validator.class);
    protected static String dbgm = "[Valid]:[idx={}]vs[dat={}]:[file]:'{}'";
    public static boolean verify(String file, Ruler rule) {
        return file.endsWith(rule.value());
    }

    @Override
    public abstract String onRule(String index, E[] files);

    /**
     * 
     */
    public final static Validator<?> VALIDATOR = new Validator<Object>() {
        @Override
        public String onRule(String index, Object[] files) {
            if (Objects.isNull(files) || files.length == 0) {
                if (verify(index, Ruler.ok)) return index;
                else if (verify(index, Ruler.txt)) return index;
            }
            else {
                int i = 0;
                while (i < files.length) try {
                    Object o = files[i++];
                    String name;
                    long size;
                    if (o instanceof File) {
                        name = ((File) o).toPath().getFileName().toString();
                        size = Files.size(((File) o).toPath());
                    }
                    else if (o instanceof Path) {
                        name = ((Path) o).getFileName().toString();
                        size = Files.size((Path) o);
                    }
                    else if (o instanceof FTPFile) {
                        name = ((FTPFile) o).getName();
                        size = ((FTPFile) o).getSize();
                    }
                    else {
                        throw new IllegalArgumentException(o.toString());
                    }
                    if (name.endsWith(getName(index))) {
                        lggr.info(dbgm, Spread.FILE_SIZE_FORMAT.format(getSize(index)),
                                Spread.FILE_SIZE_FORMAT.format(size), name);
                        if (size == getSize(index)) return index;
                    }
                }
                catch (Exception e) {
                    lggr.error(e.getMessage(), e);
                }
            }
            return null;
        }
    };

    /**
     * 
     * @param type
     * @return
     */
    public static Validator<?> getValidator(Class<?> type) {
        if (type == FTPFile.class) return new FTPFileValidator();
        else if (type == File.class) return new LocalFileValidator();
        else if (type == Path.class) return new LocalPathValidator();
        throw new IllegalArgumentException("" + type);
    }

    /**
     * 
     * @author Tiger
     *
     */
    static class FTPFileValidator extends Validator<FTPFile> {
        @Override
        public String onRule(final String index, FTPFile[] files) {
            if (files.length == 0) {// Verify a parameter conform to rule.
                if (verify(index, Ruler.ok)) return index;
                else if (verify(index, Ruler.txt)) return index;
            }
            else {// Retrieving a file from a group of files is same.
                try {
                    for (FTPFile file : files) {
                        if (file.getName().endsWith(getName(index))) {
                            lggr.info(dbgm, Spread.FILE_SIZE_FORMAT.format(getSize(index)),
                                    Spread.FILE_SIZE_FORMAT.format(file.getSize()),
                                    file.getName());
                            if (file.getSize() == getSize(index)) return index;
                        }
                    }
                }
                catch (Exception e) {
                    lggr.error(e.getMessage(), e);
                }
            }
            return null;
        }
    }

    /**
     * 
     * @author Tiger
     *
     */
    static class LocalPathValidator extends Validator<Path> {
        @Override
        public String onRule(String index, Path[] files) {
            if (files.length == 0) {// Verify a parameter conform to rule.
                if (verify(index, Ruler.ok)) return index;
                else if (verify(index, Ruler.txt)) return index;
            }
            else {
                try {
                    for (Path file : files) {
                        if (file.endsWith(getName(index))) {
                            lggr.info(dbgm, Spread.FILE_SIZE_FORMAT.format(getSize(index)),
                                    Spread.FILE_SIZE_FORMAT.format(Files.size(file)),
                                    file.normalize());
                            if (Files.size(file) == getSize(index)) return index;
                        }
                    }
                }
                catch (Exception e) {
                    lggr.error(e.getMessage(), e);
                }
            }
            return null;
        }
    }

    /**
     * 
     * @author Tiger
     *
     */
    static class LocalFileValidator extends Validator<File> {
        @Override
        public String onRule(String index, File[] files) {
            if (files.length == 0) {// Verify a parameter conform to rule.
                if (verify(index, Ruler.ok)) return index;
                else if (verify(index, Ruler.txt)) return index;
            }
            else {
                try {
                    for (File file : files) {
                        if (file.toPath().endsWith(getName(index))) {
                            lggr.info(dbgm, Spread.FILE_SIZE_FORMAT.format(getSize(index)),
                                    Spread.FILE_SIZE_FORMAT.format(file.length()),
                                    file.getAbsolutePath());
                            if (file.length() == getSize(index)) return index;
                        }
                    }
                }
                catch (Exception e) {
                    lggr.error(e.getMessage(), e);
                }
            }
            return null;
        }
    }

}
