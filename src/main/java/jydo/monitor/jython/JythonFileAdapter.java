package jydo.monitor.jython;

import java.io.File;

import org.python.core.PyCode;
import org.python.util.PythonInterpreter;

public class JythonFileAdapter implements JythonFileListener
{
    @Override
    public void fileModified(File file, PyCode pyCode, PythonInterpreter python)
    {
    }

    @Override
    public void fileRemoved(File file)
    {
    }

}
