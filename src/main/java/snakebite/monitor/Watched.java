package snakebite.monitor;

import java.util.Collection;

public abstract class Watched<T, I>
{
    private T watched;

    public Watched(T watched)
    {
        this.watched = watched;
        // initially we don't track modifications - when isModified is called it
        // will end up in the added collection
    }

    public boolean isModified()
    {
        return isModified(null, null, null);
    }

    public abstract boolean isModified(Collection<I> added,
                                       Collection<I> modified,
                                       Collection<I> removed);

    public T getWatched()
    {
        return watched;
    }
}
