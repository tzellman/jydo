package snakebite.monitor.jython;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.python.core.PyClass;
import org.python.core.PyFunction;
import org.python.core.PyIterator;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PyStringMap;
import org.python.core.PyTuple;

public class JythonUtils
{

    public static <T> List<T> filterType(PyObject locals, Class<T> clazz)
    {
        List<T> filtered = new LinkedList<T>();
        if (locals != null && PyStringMap.class.isInstance(locals))
        {
            PyStringMap dict = (PyStringMap) locals;
            Iterator it = ((PyIterator) dict.iteritems()).iterator();
            while (it.hasNext())
            {
                PyTuple t = (PyTuple) it.next();
                Object key = t.get(0);
                Object val = t.get(1);
                if (val != null)
                {
                    if (clazz.isInstance(val))
                    {
                        filtered.add((T) val);
                    }
                }
            }
        }
        return filtered;
    }

    public static List<PyFunction> filterFunctions(PyObject locals)
    {
        return filterType(locals, PyFunction.class);
    }

    public static List<PyClass> filterClasses(PyObject locals)
    {
        return filterType(locals, PyClass.class);
    }

    public static Map<String, PyObject> getPublicDir(PyObject obj)
    {
        Map<String, PyObject> items = new TreeMap<String, PyObject>();
        PyObject dir = obj.__dir__();
        if (dir.isSequenceType())
        {
            PySequence seq = (PySequence) dir;
            Iterator<Object> it = ((PyIterator) seq.__iter__()).iterator();
            while (it.hasNext())
            {
                String name = ObjectUtils.toString(it.next());
                if (!StringUtils.startsWith(name, "_"))
                {
                    try
                    {
                        PyObject item = obj.__getattr__(name);
                        items.put(name, item);
                    }
                    catch (Exception e)
                    {
                    }
                }
            }
        }
        return items;
    }

    public static Map<String, PyMethod> getMethods(PyObject obj)
    {
        Map<String, PyMethod> methods = new TreeMap<String, PyMethod>();
        Map<String, PyObject> items = getPublicDir(obj);
        Iterator<Entry<String, PyObject>> it = items.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<String, PyObject> next = it.next();
            if (PyMethod.class.isInstance(next.getValue()))
                methods.put(next.getKey(), (PyMethod) next.getValue());
        }
        return methods;
    }

    private JythonUtils()
    {
    }

}
