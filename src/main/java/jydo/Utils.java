package jydo;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public final class Utils
{

    public static File urlToFile(URL url)
    {
        try
        {
            return new File(url.toURI());
        }
        catch (URISyntaxException e)
        {
            return new File(url.getPath());
        }
    }

    public static URL fileToURL(File file) throws MalformedURLException
    {
        return file.toURI().toURL();
    }

    public static Long getLastModified(URL url)
    {
        try
        {
            return url.openConnection().getLastModified();
        }
        catch (IOException e1)
        {
            return null;
        }
    }

    private Utils()
    {
    }
}
