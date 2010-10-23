package snakebite.monitor;

import java.io.File;
import java.net.URL;

import snakebite.Utils;

public abstract class FileWatchdog extends Watchdog<File, File>
{
    protected Watched<File, File> newWatched(File file)
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
            watch(classLoader.getResource(p));
        }
    }
}
