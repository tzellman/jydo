package jydo.monitor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The job of the Watchdog class is to essentially watch a collection of items
 * and notify listeners when something changes. Currently, listeners are
 * notified when an item is (a) added, (b) removed and (c) modified.
 * 
 * @param <T_Collection>
 *            the Collection type
 * @param <T_Item>
 *            the individual item (can be the same as Collection)
 */
public abstract class Watchdog<T_Collection, T_Item>
{
    public static final int MIN_WAIT = 5;

    protected Set<Watched<T_Item>> watched;

    // protected Map<String, Object> options;

    protected int waitSeconds;

    protected Set<CollectionListener<T_Item>> listeners;

    protected Thread watchThread;

    protected boolean alive;

    protected boolean checking;

    private Lock startStopLock;

    public Watchdog()
    {
        watched = new HashSet<Watched<T_Item>>();
        // options = new HashMap<String, Object>();
        listeners = new HashSet<CollectionListener<T_Item>>();
        waitSeconds = MIN_WAIT;
        startStopLock = new ReentrantLock();
        checking = false;
    }

    /**
     * Register some Collections to be watched.
     * 
     * @param toWatch
     */
    public void watch(T_Collection... toWatch)
    {
        synchronized (watched)
        {
            for (T_Collection t : toWatch)
            {
                Watched<T_Item> item = newWatched(t);
                if (item == null)
                {
                    // TODO log warning
                }
                else
                    watched.add(item);
            }
        }
    }

    /**
     * Implementors are required to provide a way of generating a new Watched
     * object, based on the given Collection.
     * 
     * @param t
     * @return
     */
    protected abstract Watched<T_Item> newWatched(T_Collection t);

    /**
     * Set the amount of time, in seconds, to wait in-between checks.
     * 
     * @param seconds
     */
    public void setWaitSeconds(int seconds)
    {
        if (seconds < MIN_WAIT)
            seconds = MIN_WAIT;
        else
            waitSeconds = seconds;
    }

    /**
     * Register listeners
     * 
     * @param listener
     */
    public void addCollectionListener(CollectionListener<T_Item>... listener)
    {
        listeners.addAll(Arrays.asList(listener));
    }

    public void removeCollectionListener(CollectionListener<T_Item>... listener)
    {
        listeners.removeAll(Arrays.asList(listener));
    }

    /**
     * Tell the Watchdog to start watching. This is non-blocking, in that it
     * returns immediately.
     */
    public void startWatching()
    {
        startWatching(false);
    }

    /**
     * Tell the Watchdog to start watching. You have the option to run the first
     * check before returning.
     * 
     * @param block
     *            if True, the method blocks until all watched items are first
     *            checked
     */
    public void startWatching(boolean block)
    {
        startStopLock.lock();
        try
        {
            if (watchThread == null || !watchThread.isAlive())
            {
                watchThread = new Thread(makeRunnable());
                alive = true;
            }
        }
        finally
        {
            startStopLock.unlock();
        }

        if (!watchThread.isAlive())
        {
            if (block)
            {
                // this allows the caller to block until everything is
                // checked first
                check();
            }
            watchThread.start();
        }
    }

    /**
     * Manually force the Watchdog to check for changes. Calling this method
     * does not interrupt the normal watch schedule. All registered listeners
     * will get notified of changes.
     */
    public void check()
    {
        check(listeners.toArray(new CollectionListener[0]));
    }

    /**
     * Manually force the Watchdog to check for changes. Calling this method
     * does not interrupt the normal watch schedule. Only the listeners
     * passed-in to the method will get notified of changes.
     */
    public void check(CollectionListener<T_Item>... listeners)
    {
        startStopLock.lock();
        checking = true;

        try
        {
            List<T_Item> added = new LinkedList<T_Item>();
            List<T_Item> removed = new LinkedList<T_Item>();
            List<T_Item> modified = new LinkedList<T_Item>();
            synchronized (watched)
            {
                for (Watched<T_Item> item : watched)
                {
                    item.checkForChanges(added, modified, removed);
                }

                for (T_Item item : added)
                {
                    for (CollectionListener<T_Item> listener : listeners)
                    {
                        try
                        {
                            listener.itemAdded(item);
                        }
                        catch (Throwable e)
                        {
                            // TODO log this
                            e.printStackTrace();
                        }
                    }
                }
                for (T_Item item : modified)
                {
                    for (CollectionListener<T_Item> listener : listeners)
                    {
                        try
                        {
                            listener.itemUpdated(item);
                        }
                        catch (Throwable e)
                        {
                            // TODO log this
                            e.printStackTrace();
                        }
                    }
                }
                for (T_Item item : removed)
                {
                    for (CollectionListener<T_Item> listener : listeners)
                    {
                        try
                        {
                            listener.itemRemoved(item);
                        }
                        catch (Throwable e)
                        {
                            // TODO log this
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        finally
        {
            startStopLock.unlock();
            checking = false;
        }
    }

    /**
     * Stop the Watchdog from watching on a scheduled basis. You can still call
     * check() manually.
     */
    public void stopWatching()
    {
        startStopLock.lock();
        try
        {
            alive = false;
            watchThread = null;
            // might still not be thread safe
        }
        finally
        {
            startStopLock.unlock();
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        stopWatching();
    }

    /**
     * @return true if the Watchdog has been started, or if it is in the process
     *         of checking
     */
    public boolean isWatching()
    {
        return (watchThread != null && alive) || checking;
    }

    private Runnable makeRunnable()
    {
        return new Runnable()
        {
            public void run()
            {
                do
                {
                    check();
                    try
                    {
                        Thread.sleep(waitSeconds * 1000);
                    }
                    catch (InterruptedException e)
                    {
                        break;
                    }
                } while (alive);
            }
        };
    }
}
