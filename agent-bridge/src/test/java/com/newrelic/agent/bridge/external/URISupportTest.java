package com.newrelic.agent.bridge.external;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class URISupportTest {
    @Test
    public void getURI_withValidURI_returnsProperStr() throws URISyntaxException {
        URI uri = new URI("https://www.newrelic.com/pricing");
        Assert.assertEquals("https://www.newrelic.com/pricing", URISupport.getURI(uri));
    }

    @Test
    public void getURI_withValidURL_returnsProperStr() throws MalformedURLException {
        URL url = new URL("https", "www.newrelic.com", 80, "/pricing");
        Assert.assertEquals("https://www.newrelic.com:80/pricing", URISupport.getURI(url));
    }

    @Test
    public void getURI_withURLSyntaxException_returnsProperStr() throws MalformedURLException {
        String urlStr = "http://newrelic.com:80/pracing";
        Assert.assertEquals(urlStr, URISupport.getURI(new URL(urlStr+"#`")));
    }

    @Test
    public void getURI_withNullURL_returnsEmptyStr() throws MalformedURLException {
        URL url = null;
        Assert.assertEquals("", URISupport.getURI(url));
    }

    @Test
    public void getURI_withNullURI_returnsEmptyStr() throws MalformedURLException {
        URI uri = null;
        Assert.assertEquals("", URISupport.getURI(uri));
    }
}
