package jydo.monitor;

import java.util.Collection;

/**
 * Models something being watched.
 * 
 * @param <I>
 */
public interface Watched<I>
{
    /**
     * Check for changes to whatever is being watched. You can optionally pass
     * in collections that hold the results.
     * 
     * @param added
     * @param modified
     * @param removed
     * @return true if anything has been added, modified or removed; false
     *         otherwise
     */
    boolean checkForChanges(Collection<I> added, Collection<I> modified,
                            Collection<I> removed);

}
