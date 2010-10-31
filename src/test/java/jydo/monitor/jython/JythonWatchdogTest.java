package jydo.monitor.jython;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import jydo.monitor.jython.JythonFileAdapter;
import jydo.monitor.jython.JythonWatchdog;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.python.core.PyCode;
import org.python.util.PythonInterpreter;


public class JythonWatchdogTest
{

    static int SLEEP_TIME = 2000;

    protected static List<File> watchFiles(boolean watch, String... files)
    {
        JythonWatchdog watchDog = new JythonWatchdog();
        watchDog.watch(files);
        final List<File> changedFiles = new LinkedList<File>();

        watchDog.addJythonFileListener(new JythonFileAdapter()
        {
            @Override
            public void fileModified(File file, PyCode pyCode,
                                     PythonInterpreter python)
            {
                changedFiles.add(file);
                python.exec(pyCode);
            }
        });

        if (watch)
        {
            watchDog.startWatching();
            try
            {
                Thread.sleep(SLEEP_TIME);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            watchDog.stopWatching();
        }
        else
        {
            watchDog.check();
        }
        return changedFiles;
    }

    protected static List<File> checkFiles(String... files)
    {
        return watchFiles(false, files);
    }

    protected static List<File> watchFiles(String... files)
    {
        return watchFiles(true, files);
    }

    @Test
    public void checkFiles()
    {
        List<File> files = checkFiles("test2.py", "test.py");
        Assert.assertEquals(2, files.size());
    }

    @Test
    public void checkFile()
    {
        List<File> files = checkFiles("test.py");
        Assert.assertEquals(1, files.size());
    }

    @Test
    public void checkDir()
    {
        List<File> files = checkFiles(".");
        Assert.assertTrue(files.size() >= 2);
    }

    @Test
    public void watchFiles()
    {
        List<File> files = watchFiles("test2.py", "test.py");
        Assert.assertEquals(2, files.size());
    }

    @Test
    public void watchFile()
    {
        List<File> files = watchFiles("test.py");
        Assert.assertEquals(1, files.size());
    }

    @Test
    public void watchDir()
    {
        List<File> files = watchFiles(".");
        Assert.assertTrue(files.size() >= 2);
    }

    @Test
    public void restartWatchdog()
    {
        JythonWatchdog watchDog = new JythonWatchdog();
        watchDog.startWatching();
        watchDog.stopWatching();
        watchDog.startWatching();
        watchDog.stopWatching();
    }

    @Test
    public void startTwice()
    {
        JythonWatchdog watchDog = new JythonWatchdog();
        watchDog.startWatching();
        watchDog.startWatching();
        watchDog.stopWatching();
    }

    // @Test
    public void runCode()
    {
    }

}
