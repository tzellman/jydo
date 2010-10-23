package snakebite.monitor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Watchdog<T_Collection, T_Item>
{
    public static final int MIN_WAIT = 5;

    protected Set<Watched<T_Collection, T_Item>> watched;

    // protected Map<String, Object> options;

    protected int waitSeconds;

    protected Set<CollectionListener<T_Item>> listeners;

    protected Thread watchThread;

    protected boolean alive;

    private Lock startStopLock;

    public Watchdog()
    {
        watched = new HashSet<Watched<T_Collection, T_Item>>();
        // options = new HashMap<String, Object>();
        listeners = new HashSet<CollectionListener<T_Item>>();
        waitSeconds = MIN_WAIT;
        startStopLock = new ReentrantLock();
    }

    public void watch(T_Collection... toWatch)
    {
        synchronized (watched)
        {
            for (T_Collection t : toWatch)
            {
                Watched<T_Collection, T_Item> item = newWatched(t);
                if (item == null)
                {
                    // TODO log warning
                }
                else
                    watched.add(item);
            }
        }
    }

    protected abstract Watched<T_Collection, T_Item> newWatched(T_Collection t);

    // public void watch(URL... urls)
    // {
    // for (URL u : urls)
    // {
    // watch(Utils.urlToFile(u));
    // }
    // }
    //
    // public void watch(String... pathnames)
    // {
    // ClassLoader classLoader = getClass().getClassLoader();
    // for (String p : pathnames)
    // {
    // watch(classLoader.getResource(p));
    // }
    // }

    public void setWaitSeconds(int seconds)
    {
        if (seconds < MIN_WAIT)
            seconds = MIN_WAIT;
        else
            waitSeconds = seconds;
    }

    public void addFileChangeListener(CollectionListener<T_Item> listener)
    {
        listeners.add(listener);
    }

    public void removeFileChangeListener(CollectionListener<T_Item> listener)
    {
        listeners.remove(listener);
    }

    public void startWatching()
    {
        startStopLock.lock();
        try
        {
            if (watchThread == null)
            {
                watchThread = new Thread(makeRunnable());
                alive = true;
                watchThread.start();
            }
        }
        finally
        {
            startStopLock.unlock();
        }
    }

    public void stopWatching()
    {
        startStopLock.lock();
        try
        {
            alive = false;
            // watchThread = null;
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

    public boolean isWatching()
    {
        startStopLock.lock();
        try
        {
            return watchThread != null && alive;
        }
        finally
        {
            startStopLock.unlock();
        }
    }

    private Runnable makeRunnable()
    {
        return new Runnable()
        {
            public void run()
            {
                List<T_Item> added = new LinkedList<T_Item>();
                List<T_Item> removed = new LinkedList<T_Item>();
                List<T_Item> modified = new LinkedList<T_Item>();
                do
                {
                    added.clear();
                    removed.clear();
                    modified.clear();
                    synchronized (watched)
                    {
                        for (Watched<T_Collection, T_Item> item : watched)
                        {
                            item.isModified(added, modified, removed);
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
