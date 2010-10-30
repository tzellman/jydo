package snakebite.web.jython;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.NotNullPredicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyFunction;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyObjectDerived;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import snakebite.monitor.jython.JythonFileListener;
import snakebite.monitor.jython.JythonUtils;
import snakebite.monitor.jython.JythonWatchdog;

import com.meterware.httpunit.HttpException;

public class JythonServlet extends HttpServlet implements JythonFileListener
{
    protected JythonWatchdog watchDog;

    protected Map<String, List<PyObject>> mapping;

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        mapping = new TreeMap<String, List<PyObject>>();

        watchDog = new JythonWatchdog();

        String toWatch = config.getInitParameter("watch");
        if (toWatch != null)
        {
            String[] filenames = StringUtils.split(toWatch, ",");
            watchDog.watch(filenames);
        }

        String checkEvery = config.getInitParameter("checkEvery");
        try
        {
            watchDog.setWaitSeconds(Integer.parseInt(checkEvery));
        }
        catch (Exception e)
        {
        }

        watchDog.addJythonFileListener(this);
        watchDog.startWatching(true);
    }

    @Override
    public void fileModified(File file, PyCode pyCode, PythonInterpreter python)
    {
        String filePath = file.getAbsolutePath();
        python.exec(pyCode);

        try
        {
            List<PyObject> objects = new LinkedList<PyObject>();
            objects.addAll(JythonUtils.filterFunctions(python.getLocals()));
            // objects.addAll(JythonUtils.filterClasses(python.getLocals()));
            synchronized (mapping)
            {
                mapping.put(filePath, objects);
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
        synchronized (mapping)
        {
            // unregister any urls for this file
            mapping.remove(file.getAbsolutePath());
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
        String servletPath = request.getServletPath();
        List<String> parts = splitRequestContext(servletPath);

        // first collect all the possible endpoints
        List<PyObject> choices = new LinkedList<PyObject>();
        synchronized (mapping)
        {
            for (List<PyObject> objects : mapping.values())
            {
                CollectionUtils.select(objects, new Predicate()
                {
                    @Override
                    public boolean evaluate(Object obj)
                    {
                        return obj != null && PyFunction.class.isInstance(obj);
                    }
                }, choices);
            }
        }

        if (parts.isEmpty())
        {
            if (!executeSpecialMethods(choices, "", request, response))
                throw new HTTPException(404);
        }

        while (!parts.isEmpty())
        {
            String urlPart = parts.remove(0);

            boolean done = false;
            boolean skipPart = false;
            Iterator<PyObject> it = choices.iterator();
            while (!skipPart && !done && it.hasNext())
            {
                PyObject choice = it.next();
                PyFunction func = null;
                if (PyMethod.class.isInstance(choice))
                {
                    PyMethod m = (PyMethod) choice;
                    func = (PyFunction) m.im_func;
                }
                else if (PyFunction.class.isInstance(choice))
                    func = (PyFunction) choice;

                if (func != null)
                {
                    if (func.__name__.equals(urlPart))
                    {
                        PyObject val = null;
                        try
                        {
                            // use choice since it could be a PyMethod
                            val = executeAndSerialize(choice, request, response);
                            if (val == null)
                                done = true;
                        }
                        catch (Exception e)
                        {
                        }

                        if (val != null)
                        {
                            if (val.isCallable())
                            {
                                if (PyFunction.class.isInstance(val))
                                {
                                    choices.clear();
                                    choices.add(val);
                                    skipPart = true;
                                    break;
                                }
                                else
                                {
                                    // Not sure ...
                                    throw new HTTPException(500);
                                }
                            }
                            else
                            {
                                // last ditch, maybe it is an instance of a
                                // class that has methods we can use
                                if (PyObjectDerived.class.isInstance(val))
                                {
                                    choices.clear();
                                    choices.addAll(JythonUtils.getMethods(val)
                                            .values());
                                    // if (!choices.isEmpty())
                                    // {
                                    skipPart = true;
                                    break;
                                    // }
                                }

                                log("Error: Unable to map type: " + val + " : "
                                        + val.getClass().getName());

                                throw new HTTPException(500);
                            }
                        }
                    }
                    else if (func.__name__.equals(getActionName(urlPart)))
                    {
                        try
                        {
                            executeFunc(choice, Py.java2py(request),
                                    Py.java2py(response));
                            done = true;
                        }
                        catch (Exception e)
                        {
                            log(ExceptionUtils.getStackTrace(e));
                            throw new HTTPException(500);
                        }
                    }
                }
            }

            if (!skipPart && !done)
            {
                done = executeSpecialMethods(choices,
                        StringUtils.join(parts, "/"), request, response);
            }

            if (!skipPart && !done)
            {
                throw new HTTPException(404);
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

    public static List<String> splitRequestContext(String context)
    {
        return new LinkedList<String>(CollectionUtils.predicatedCollection(
                Arrays.asList(StringUtils.split(context, '/')), new Predicate()
                {
                    @Override
                    public boolean evaluate(Object arg)
                    {
                        return !StringUtils.isBlank(ObjectUtils.toString(arg,
                                ""));
                    }
                }));
    }

    private static String getActionName(String name)
    {
        return "do" + StringUtils.capitalize(name);
    }

    private boolean handleResult(PyObject result, HttpServletResponse response)
    {
        if (PyString.class.isInstance(result))
        {
            // we're done...
            try
            {
                IOUtils.write(result.asString(), response.getOutputStream());
                return true;
            }
            catch (Exception e)
            {
            }
        }

        // try some supported types
        try
        {
            InputStream stream = Py.tojava(result, InputStream.class);
            IOUtils.copy(stream, response.getOutputStream());
            IOUtils.closeQuietly(stream);
            return true;
        }
        catch (Exception e)
        {
        }

        return false;
    }

    /**
     * Attempts to execute the function. An exception will be thrown if it can't
     * handle it. If the result of the method can be serialized, it will be and
     * null will be returned. If the result of the method cannot be serialized,
     * it will be returned.
     * 
     * @param func
     * @param self
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    private PyObject executeAndSerialize(PyObject func,
                                         HttpServletRequest request,
                                         HttpServletResponse response)
            throws Exception
    {

        try
        {
            PyObject val = null;
            // Try the simple case first
            val = executeFunc(func);
            if (val == null)
                throw new Exception("NULL result...");
            else
            {
                if (handleResult(val, response))
                    return null;
                else
                    return val;
            }
        }
        catch (Exception e)
        {
            // Try passing the request/response
            executeFunc(func, Py.java2py(request), Py.java2py(response));
            return null;
        }
    }

    private boolean executeSpecialMethods(List<PyObject> choices,
                                          String context,
                                          HttpServletRequest request,
                                          HttpServletResponse response)
            throws HttpException
    {
        // TODO check for special methods: index/dynamic
        boolean done = false;
        PyObject indexFunc = (PyObject) CollectionUtils.find(choices,
                new FunctionNamePredicate("index"));
        PyObject doIndexFunc = (PyObject) CollectionUtils.find(choices,
                new FunctionNamePredicate("doIndex"));
        PyObject doDynamicFunc = (PyObject) CollectionUtils.find(choices,
                new FunctionNamePredicate("doDynamic"));

        List<PyObject> order = new LinkedList<PyObject>();
        if (StringUtils.isBlank(context))
        {
            order.add(indexFunc);
            order.add(doIndexFunc);
            order.add(doDynamicFunc);
        }
        else
        {
            order.add(doDynamicFunc);
            order.add(indexFunc);
            order.add(doIndexFunc);
        }

        CollectionUtils.filter(order, NotNullPredicate.INSTANCE);

        for (Iterator<PyObject> it = order.iterator(); !done && it.hasNext();)
        {
            try
            {
                executeAndSerialize(it.next(), request, response);
                done = true;
            }
            catch (Exception e)
            {
            }
        }
        return done;
    }

    private PyObject executeFunc(PyObject func, PyObject... args)
            throws Exception
    {
        List<PyObject> argList = new LinkedList<PyObject>(Arrays.asList(args));
        if (PyMethod.class.isInstance(func))
        {
            PyMethod m = (PyMethod) func;
            argList.add(0, m.im_self);
            func = m.im_func;
        }
        if (PyFunction.class.isInstance(func))
        {
            return func.__call__(argList.toArray(new PyObject[0]));
        }
        else
            throw new Exception("Not a Function");
    }

    class FunctionNamePredicate implements Predicate
    {
        private Set<String> names;

        public FunctionNamePredicate(String... names)
        {
            this.names = new HashSet<String>();
            this.names.addAll(Arrays.asList(names));
        }

        @Override
        public boolean evaluate(Object obj)
        {
            if (PyFunction.class.isInstance(obj))
            {
                String name = ((PyFunction) obj).__name__;
                if (names.contains(name))
                    return true;
            }
            if (PyMethod.class.isInstance(obj))
            {
                String name = ((PyFunction) ((PyMethod) obj).im_func).__name__;
                if (names.contains(name))
                    return true;
            }
            return false;
        }
    }

}
