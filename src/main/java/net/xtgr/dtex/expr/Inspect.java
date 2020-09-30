/**
 * 
 */

package net.xtgr.dtex.expr;

import java.io.IOException;

/**
 * 
 * @author Tiger
 *
 */
public interface Inspect extends Transport {

    /**
     * 
     * @return
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    java.util.Queue<String[]> retrieveAvailable() throws IOException, Exception;

    @Override
    <E> void applies(E applier) throws Exception;

    @Override
    void run();

}
