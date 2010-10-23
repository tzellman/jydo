package snakebite.monitor.jython;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.python.core.PyCode;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import snakebite.monitor.CollectionListener;
import snakebite.monitor.FileWatchdog;
import snakebite.monitor.WatchedFile;

public class JythonWatchdog extends FileWatchdog implements
        CollectionListener<File>
{
    private static class JythonResource extends WatchedFile
    {
        PyCode pyCode;

        boolean notifiedLoad = false;

        public JythonResource(File file, PyCode pyCode)
        {
            super(file);
            this.pyCode = pyCode;
        }

        public PyCode getPyCode()
        {
            return pyCode;
        }
    }

    protected PythonInterpreter python;

    protected Map<String, JythonResource> jythonFiles;

    protected Set<JythonFileListener> jythonFileListeners;

    public JythonWatchdog()
    {
        python = new PythonInterpreter();
        jythonFiles = new TreeMap<String, JythonResource>();
        jythonFileListeners = Collections
                .synchronizedSet(new HashSet<JythonFileListener>());
        super.addFileChangeListener(this);
    }

    @Override
    public void itemUpdated(File file)
    {
        watchFile(file);
    }

    @Override
    public void itemAdded(File file)
    {
        watchFile(file);
    }

    protected void watchFile(File file)
    {
        if (StringUtils.endsWith(file.getName(), ".py"))
        {
            try
            {
                loadJythonFile(file);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void itemRemoved(File file)
    {
        synchronized (jythonFiles)
        {
            String path = file.getAbsolutePath();
            if (jythonFiles.containsKey(path))
            {
                jythonFiles.remove(path);
                for (JythonFileListener listener : jythonFileListeners)
                {
                    try
                    {
                        listener.fileRemoved(file);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void notifyFileModified(File file)
    {
        synchronized (python)
        {
            for (JythonFileListener listener : jythonFileListeners)
            {
                try
                {
                    listener.fileModified(
                            file,
                            jythonFiles.get(file.getAbsolutePath()).getPyCode(),
                            python);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    python.cleanup();
                    python.setLocals(new PyDictionary());
                }
            }
        }
    }

    protected void loadJythonFile(File file) throws IOException
    {
        synchronized (jythonFiles)
        {
            String filePath = file.getAbsolutePath();

            if (!file.exists() || file.isDirectory())
            {
                jythonFiles.remove(filePath);
                return;
            }

            JythonResource jythonFile = jythonFiles.get(filePath);
            PyCode pyCode = jythonFile != null ? jythonFile.pyCode : null;

            if (jythonFile == null || !jythonFile.isModified())
            {
                String codeString = FileUtils.readFileToString(file);
                synchronized (python)
                {
                    pyCode = python.compile(codeString, filePath);
                }

                if (jythonFile == null)
                {
                    jythonFile = new JythonResource(file, pyCode);
                    jythonFiles.put(filePath, jythonFile);
                }
                else
                    jythonFile.pyCode = pyCode;

                // if we are currently "watching"
                if (isWatching())
                {
                    // notify listeners that it was loaded
                    jythonFile.notifiedLoad = true;
                    notifyFileModified(file);
                }
            }
        }
    }

    @Override
    public void startWatching()
    {
        super.startWatching();
        synchronized (jythonFiles)
        {
            for (JythonResource jythonFile : jythonFiles.values())
            {
                if (!jythonFile.notifiedLoad)
                {
                    // notify listeners of the initial load
                    jythonFile.notifiedLoad = true;
                    notifyFileModified(jythonFile.getWatched());
                }
            }
        }
    }

    public void addJythonFileListener(JythonFileListener listener)
    {
        jythonFileListeners.add(listener);
    }

    public void removeJythonFileListener(JythonFileListener listener)
    {
        jythonFileListeners.remove(listener);
    }

    public static void main(String[] args) throws Exception
    {
        JythonWatchdog watchDog = new JythonWatchdog();
        watchDog.watch("test2.py", "test.py");

        watchDog.addJythonFileListener(new JythonFileAdapter()
        {
            @Override
            public void fileModified(File file, PyCode pyCode,
                                     PythonInterpreter python)
            {
                System.out.println("Python File: " + file.getAbsolutePath());
                python.exec(pyCode);
            }
        });

        // FileUtils.touch(file);
        watchDog.startWatching();

        Thread.sleep(5000);

        watchDog.stopWatching();
    }

}
