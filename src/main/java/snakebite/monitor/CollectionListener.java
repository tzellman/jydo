package snakebite.monitor;

/**
 * Interface for listening to changes made to some sort of Collection of items
 */
public interface CollectionListener<T>
{
    public void itemAdded(T item);

    public void itemUpdated(T item);

    public void itemRemoved(T item);
}
