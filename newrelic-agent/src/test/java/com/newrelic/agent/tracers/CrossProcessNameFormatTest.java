/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.util.Obfuscator;

public class CrossProcessNameFormatTest {

    @Test
    public void test() {
        String host = "www.example.com";
        String encodingKey = "test";
        String obfuscatedAppData = Obfuscator.obfuscateNameUsingKey(
                "[\"6#66\",\"WebTransaction\\/test\\/test\",1.0,0.2,12345]", encodingKey);
        String uri = "http://www.example.com";

        CrossProcessNameFormat crossProcessNameFormat = CrossProcessNameFormat.create(host, uri, obfuscatedAppData,
                encodingKey);

        String expected = "ExternalApp/www.example.com/6#66/all";
        Assert.assertEquals(expected, crossProcessNameFormat.getHostCrossProcessIdRollupMetricName());

        expected = "ExternalTransaction/www.example.com/6#66/WebTransaction/test/test";
        Assert.assertEquals(expected, crossProcessNameFormat.getMetricName());
        Assert.assertEquals(expected, crossProcessNameFormat.getTransactionSegmentName());
        Assert.assertEquals(uri, crossProcessNameFormat.getTransactionSegmentUri());
    }

    @Test
    public void testGuid() {
        String host = "www.example.com";
        String encodingKey = "test";
        String obfuscatedAppData = Obfuscator.obfuscateNameUsingKey(
                "[\"6#66\",\"WebTransaction\\/test\\/test\",1.0,0.2,12345,\"ee8e5ef1a374c0ec\"]", encodingKey);
        String uri = "http://www.example.com:80";

        CrossProcessNameFormat crossProcessNameFormat = CrossProcessNameFormat.create(host, uri, obfuscatedAppData,
                encodingKey);

        String expected = "ExternalApp/www.example.com/6#66/all";
        Assert.assertEquals(expected, crossProcessNameFormat.getHostCrossProcessIdRollupMetricName());

        expected = "ExternalTransaction/www.example.com/6#66/WebTransaction/test/test";
        Assert.assertEquals(expected, crossProcessNameFormat.getMetricName());
        Assert.assertEquals(expected, crossProcessNameFormat.getTransactionSegmentName());
        Assert.assertEquals(uri, crossProcessNameFormat.getTransactionSegmentUri());
        Assert.assertEquals("ee8e5ef1a374c0ec", crossProcessNameFormat.getTransactionId());
    }

    @Test
    public void testNoQueryParametersURI() {
        String host = "www.example.com";
        String encodingKey = "test";
        String obfuscatedAppData = Obfuscator.obfuscateNameUsingKey(
                "[\"6#66\",\"WebTransaction\\/test\\/test\",1.0,0.2,12345,\"ee8e5ef1a374c0ec\"]", encodingKey);
        String uri = "http://www.example.com:80";
        String queryParams = "?mySensitive=data";

        CrossProcessNameFormat crossProcessNameFormat = CrossProcessNameFormat.create(host, uri + queryParams, obfuscatedAppData,
                encodingKey);
        Assert.assertEquals(uri, crossProcessNameFormat.getTransactionSegmentUri());
    }

    @Test
    public void testNullTransactionName() {
        String host = "www.example.com";
        String encodingKey = "test";
        String obfuscatedAppData = Obfuscator.obfuscateNameUsingKey(
                "[\"6#66\",null,1.0,0.2,12345,\"ee8e5ef1a374c0ec\"]", encodingKey);
        String uri = "http://www.example.com:80";
        String queryParams = "?mySensitive=data";

        CrossProcessNameFormat crossProcessNameFormat = CrossProcessNameFormat.create(host, uri + queryParams, obfuscatedAppData,
                encodingKey);
        
        // Since we used invalid APP_DATA (null transactionName) we should get back a null CrossProcessNameFormat
        Assert.assertNull(crossProcessNameFormat);
    }
}
