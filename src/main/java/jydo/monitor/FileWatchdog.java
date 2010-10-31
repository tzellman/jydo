package jydo.monitor;

import java.io.File;
import java.net.URL;

import jydo.Utils;

/**
 * A Watchdog implementation that watches a set of Files.
 */
public abstract class FileWatchdog extends Watchdog<File, File>
{
    protected Watched<File> newWatched(File file)
    {
        return new WatchedFile(file);
    }

    public void watch(URL... urls)
    {
        for (URL u : urls)
        {
            watch(Utils.urlToFile(u));
        }
    }

    public void watch(String... pathnames)
    {
        ClassLoader classLoader = getClass().getClassLoader();
        for (String p : pathnames)
        {
            try
            {
                watch(classLoader.getResource(p));
            }
            catch (Exception e)
            {
                watch(new File(p));
            }
        }
    }
}
