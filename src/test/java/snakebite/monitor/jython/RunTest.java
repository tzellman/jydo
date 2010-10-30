package snakebite.monitor.jython;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.python.core.PyClass;
import org.python.core.PyCode;
import org.python.core.PyFunction;
import org.python.util.PythonInterpreter;

public class RunTest
{

    @Test
    public void runCode()
    {

        JythonWatchdog watchDog = new JythonWatchdog();
        watchDog.watch("views.py");

        JythonFileAdapter listener = new JythonFileAdapter()
        {
            @Override
            public void fileModified(File file, PyCode pyCode,
                                     PythonInterpreter python)
            {
                python.exec(pyCode);
                List<PyFunction> funcs = JythonUtils.filterFunctions(python
                        .getLocals());
                Iterator<PyFunction> funcIt = funcs.iterator();
                while (funcIt.hasNext())
                {
                    PyFunction func = funcIt.next();
                    System.out.println(func);
                    System.out.println(func.getFuncDict());
                    System.out.println(func.getFuncDefaults());
                    System.out.println(func.getFuncName());
                    System.out.println(func.getFuncDoc());
                    System.out.println(func.func_closure);
                    System.out.println(func.__module__);
                }

                List<PyClass> classes = JythonUtils.filterType(
                        python.getLocals(), PyClass.class);
                Iterator<PyClass> classIt = classes.iterator();
                while (classIt.hasNext())
                {
                    PyClass clazz = classIt.next();
                    System.out.println(clazz.__name__);
                }
            }
        };

        watchDog.addJythonFileListener(listener);
        watchDog.check();
    }

}
