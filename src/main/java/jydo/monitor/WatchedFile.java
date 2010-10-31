package jydo.monitor;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

public class WatchedFile implements Watched<File>
{
    private Long lastTouched = null;

    private Map<String, Long> fileList = null;

    private File watchedFile;

    public WatchedFile(File file)
    {
        this.watchedFile = file;
    }

    @Override
    public synchronized boolean checkForChanges(Collection<File> added,
                                                Collection<File> modified,
                                                Collection<File> removed)
    {
        boolean changed = false;

        if (fileList != null)
        {
            // go through the current files and track if they've changed
            Iterator<Entry<String, Long>> it = fileList.entrySet().iterator();
            while (it.hasNext())
            {
                Entry<String, Long> entry = it.next();
                File f = new File(entry.getKey());
                if (f.exists())
                {
                    long lastModified = f.lastModified();
                    if (lastModified > entry.getValue())
                    {
                        entry.setValue(lastModified);
                        if (modified != null)
                            modified.add(f);
                        changed = true;
                    }
                }
                else if (lastTouched != null)
                {
                    // it was deleted
                    it.remove();
                    if (removed != null)
                        removed.add(f);
                    changed = true;
                }
            }
        }

        if (watchedFile.isDirectory())
        {
            Collection<File> currentList = FileUtils.listFiles(watchedFile,
                    FileFilterUtils.trueFileFilter(),
                    FileFilterUtils.falseFileFilter());

            if (fileList == null)
                fileList = new HashMap<String, Long>();
            for (File f : currentList)
            {
                String path = f.getAbsolutePath();
                if (!fileList.containsKey(path))
                {
                    if (added != null)
                        added.add(f);
                    fileList.put(path, f.lastModified());
                    changed = true;
                }
            }
        }
        else
        {
            if (watchedFile.exists())
            {
                long lastModified = watchedFile.lastModified();
                if (lastTouched == null)
                {
                    if (added != null)
                        added.add(watchedFile);
                    lastTouched = lastModified;
                    changed = true;
                }
                else if (lastModified > lastTouched)
                {
                    if (modified != null)
                        modified.add(watchedFile);
                    lastTouched = lastModified;
                    changed = true;
                }
                if (fileList != null)
                    fileList.clear();
            }
            else
            {
                if (removed != null)
                    removed.add(watchedFile);
                lastTouched = null; // mark that we recognized it's gone
                changed = true;
            }
        }
        return changed;
    }

    public boolean isModified()
    {
        return checkForChanges(null, null, null);
    }

}
