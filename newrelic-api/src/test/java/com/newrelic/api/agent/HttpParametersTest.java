package com.newrelic.api.agent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class HttpParametersTest {
    ConcurrentHashMapHeaders headers;

    @Before
    public void setup() {
        headers = ConcurrentHashMapHeaders.build(HeaderType.HTTP);
        headers.addHeader("key1", "val1");
        headers.addHeader("key2", "val2");
    }

    @Test
    public void constructor_withIndividualParams_constructsObjCorrectly() throws URISyntaxException {
        URI uri = new URI("http://www.newrelic.com");
        HttpParameters params = new HttpParameters("lib", uri, "proc", headers);
        Assert.assertEquals("lib", params.getLibrary());
        Assert.assertEquals("proc", params.getProcedure());
        Assert.assertEquals(uri, params.getUri());
        Assert.assertNull(params.getStatusCode());
        Assert.assertNull(params.getStatusText());
    }


    @Test
    public void constructor_usingBuilder_constructsObjCorrectly() throws URISyntaxException {
        HttpParameters params = new HttpParameters.Builder("lib").inboundHeaders(headers).status(200, "OK").build();
        Assert.assertEquals("lib", params.getLibrary());
        Assert.assertEquals(200, params.getStatusCode().intValue());
        Assert.assertEquals("OK", params.getStatusText());
    }
}
