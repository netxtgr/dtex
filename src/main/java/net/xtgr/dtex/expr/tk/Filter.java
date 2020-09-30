/**
 * 
 */

package net.xtgr.dtex.expr.tk;

/**
 * 
 * @author Tiger
 *
 * @param <E>
 */
public interface Filter<E> {
    boolean accept(E entity);
}
