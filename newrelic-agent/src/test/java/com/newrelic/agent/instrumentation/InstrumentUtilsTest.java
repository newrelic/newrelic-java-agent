/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Assert;

import org.junit.Test;

/**
 * Tests the utility methods in the class InstrumentUtils.
 * 
 * @since Dec 17, 2012
 */
public class InstrumentUtilsTest {

    @Test
    public void testURI() throws URISyntaxException {
        String input = "http://www.news.example.com:80/world";
        URI uri = new URI(input);
        String actual = InstrumentUtils.getURI(uri);
        Assert.assertEquals(input, actual);

        input = "http://www.news.example.com:80/world";
        uri = new URI(input + "?password=open;user=me");
        actual = InstrumentUtils.getURI(uri);
        Assert.assertEquals(input, actual);

        input = "http://www.news.example.com/world";
        uri = new URI(input + "?password=open;user=me");
        actual = InstrumentUtils.getURI(uri);
        Assert.assertEquals(input, actual);

        input = "http://127.0.0.1:8181/world";
        uri = new URI(input + "?password=open;user=me");
        actual = InstrumentUtils.getURI(uri);
        Assert.assertEquals(input, actual);
    }

    @Test
    public void testURL() throws MalformedURLException {

        String input = "http://www.news.example.com:80/world";
        URL uri = new URL(input);
        String actual = InstrumentUtils.getURI(uri);
        Assert.assertEquals(input, actual);

        input = "http://www.news.example.com:80/world";
        uri = new URL(input + "?password=open;user=me");
        actual = InstrumentUtils.getURI(uri);
        Assert.assertEquals(input, actual);

        input = "http://www.news.example.com/world";
        uri = new URL(input + "?password=open;user=me");
        actual = InstrumentUtils.getURI(uri);
        Assert.assertEquals(input, actual);

        input = "http://127.0.0.1:4040/world";
        uri = new URL(input + "?password=open;user=me");
        actual = InstrumentUtils.getURI(uri);
        Assert.assertEquals(input, actual);

    }

}
