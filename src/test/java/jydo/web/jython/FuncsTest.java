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

public class FuncsTest
{
    ServletRunner sr;

    ServletUnitClient sc;

    @Before
    public void setUp() throws Exception
    {
        sr = new ServletRunner(getClass().getClassLoader().getResourceAsStream(
                "web-funcs.xml"));
        sc = sr.newClient();
    }

    @Test
    public void testDo() throws Exception
    {
        WebRequest request = new PostMethodWebRequest(
                "http://example.com/fooBar");
        WebResponse response = sc.getResponse(request);
        assertNotNull(response);
        assertEquals("foobar", response.getText());
    }

    @Test
    public void testStream() throws Exception
    {
        WebRequest request = new PostMethodWebRequest(
                "http://example.com/bazStream");
        WebResponse response = sc.getResponse(request);
        assertNotNull(response);
        assertEquals("bazStream", response.getText());
    }

    @Test
    public void testCallable() throws Exception
    {
        WebRequest request = new PostMethodWebRequest(
                "http://example.com/foo/bar");
        WebResponse response = sc.getResponse(request);
        assertNotNull(response);
        assertEquals("bar", response.getText());
    }

    @Test
    public void testIndex() throws Exception
    {
        WebRequest request = new PostMethodWebRequest("http://example.com/");
        WebResponse response = sc.getResponse(request);
        assertNotNull(response);
        assertEquals("index", response.getText());
    }

}
