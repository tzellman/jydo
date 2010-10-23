package snakebite.web;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyFunction;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import snakebite.monitor.jython.JythonFileListener;
import snakebite.monitor.jython.JythonWatchdog;

public class JythonServlet extends HttpServlet implements JythonFileListener
{
    protected JythonWatchdog watchDog;

    protected Map<String, PyList> urls;

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        urls = new TreeMap<String, PyList>();

        watchDog = new JythonWatchdog();

        String filesParam = config.getInitParameter("files");
        if (filesParam != null)
        {
            String[] filenames = StringUtils.split(filesParam, ",");
            watchDog.watch(filenames);
        }

        String pollParam = config.getInitParameter("pollForChanges");
        try
        {
            watchDog.setWaitSeconds(Integer.parseInt(pollParam));
        }
        catch (Exception e)
        {
        }

        watchDog.addJythonFileListener(this);
        watchDog.startWatching();
    }

    @Override
    public void fileModified(File file, PyCode pyCode, PythonInterpreter python)
    {
        String filePath = file.getAbsolutePath();
        python.exec(pyCode);

        try
        {
            PyList urlList = (PyList) python.get("urls");
            if (urlList != null)
            {
                synchronized (urls)
                {
                    urls.put(filePath, urlList);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void fileRemoved(File file)
    {
        synchronized (urls)
        {
            // unregister any urls for this file
            urls.remove(file.getAbsolutePath());
        }

    }

    @Override
    public void destroy()
    {
        super.destroy();
        watchDog.stopWatching();
    }

    @Override
    protected void service(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException
    {
        route(request, response);
    }

    protected void route(HttpServletRequest request,
                         HttpServletResponse response) throws IOException
    {
        String contextPath = getContextPath(request);
        String method = request.getMethod();
        System.out.println(contextPath);
        synchronized (urls)
        {
            for (PyList urlList : urls.values())
            {
                for (Iterator<PyObject> it = urlList.iterator(); it.hasNext();)
                {
                    try
                    {
                        PyObject obj = it.next();
                        if (obj.isSequenceType())
                        {
                            PySequence seq = (PySequence) obj;
                            int len = seq.__len__();
                            if (len >= 2)
                            {
                                int index = 0;
                                PyString meth = null;
                                if (len > 2)
                                {
                                    meth = (PyString) seq.__getitem__(index++);
                                }
                                PyString url = (PyString) seq
                                        .__getitem__(index++);

                                System.out.println(url + ", " + meth);

                                if ((meth == null || StringUtils
                                        .equalsIgnoreCase(meth.toString(),
                                                method))
                                        && contextPath.matches(url.toString()))
                                {
                                    // TODO use pattern to get matcher
                                    PyFunction func = (PyFunction) seq
                                            .__getitem__(index++);

                                    func.__call__(Py.java2py(request),
                                            Py.java2py(response));

                                    // return (ResponseContext)
                                    // func.__call__(Py.java2py(requestContext))
                                    // .__tojava__(ResponseContext.class);
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {

                    }
                }
            }
        }
    }

    public static String getContextPath(HttpServletRequest request)
            throws IOException
    {
        String path = request.getRequestURI().replaceAll(
                request.getContextPath() + request.getServletPath() + "[//]?",
                "");
        return URLDecoder.decode(path, "utf-8");
    }
}
