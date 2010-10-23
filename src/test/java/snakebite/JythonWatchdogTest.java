package snakebite;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.python.core.PyCode;
import org.python.util.PythonInterpreter;

import snakebite.monitor.jython.JythonFileAdapter;
import snakebite.monitor.jython.JythonWatchdog;

public class JythonWatchdogTest
{

    protected static List<File> watchFiles(String... files)
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

        watchDog.startWatching();
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }
        watchDog.stopWatching();
        return changedFiles;
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

}
