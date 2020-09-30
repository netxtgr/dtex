/**
 * 
 */

package net.xtgr.dtex.expr;

/**
 * @author Tiger
 *
 */
public interface Handle extends Transport {

    @Override
    <E> void applies(E applier) throws Exception;

    @Override
    void run();

}
