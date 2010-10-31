package jydo.web.jython;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

public class ClassesTest
{
    ServletRunner sr;

    ServletUnitClient sc;

    @Before
    public void setUp() throws Exception
    {
        sr = new ServletRunner(getClass().getClassLoader().getResourceAsStream(
                "web-classes.xml"));
        sc = sr.newClient();
    }

    @Test
    public void testCallable() throws Exception
    {
        WebRequest request = new PostMethodWebRequest(
                "http://example.com/route/foo");
        WebResponse response = sc.getResponse(request);
        assertNotNull(response);
        assertEquals("foo", response.getText());
    }

    @Test
    public void testDoIndex() throws Exception
    {
        WebRequest request = new PostMethodWebRequest("http://example.com/");
        WebResponse response = sc.getResponse(request);
        assertNotNull(response);
        assertEquals("doIndex", response.getText());
    }

}
