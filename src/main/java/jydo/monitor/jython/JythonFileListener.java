package jydo.monitor.jython;

import java.io.File;

import org.python.core.PyCode;
import org.python.util.PythonInterpreter;

public interface JythonFileListener
{
    public void fileModified(File file, PyCode pyCode, PythonInterpreter python);

    public void fileRemoved(File file);
}
