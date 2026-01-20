/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import org.junit.Assert;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class ServiceUtilsTest {

    @Test
    public void testCalculatePathHash() {
        int step1 = ServiceUtils.calculatePathHash("test", "step1", 0);
        int step2 = ServiceUtils.calculatePathHash("test", "step2", step1);
        int step3 = ServiceUtils.calculatePathHash("test", "step3", step2);
        int step4 = ServiceUtils.calculatePathHash("test", "step4", step3);

        Assert.assertEquals("00000000", ServiceUtils.intToHexString(0));
        Assert.assertEquals(step1, ServiceUtils.reversePathHash("test", "step2", step2));
        Assert.assertEquals(step2, ServiceUtils.reversePathHash("test", "step3", step3));
        Assert.assertEquals(step3, ServiceUtils.reversePathHash("test", "step4", step4));

        System.out.println("hexStringToInt(0) = " + ServiceUtils.intToHexString(0));
        System.out.println("hexStringToInt(calculatePathHash('test','23547',0)) = "
                + ServiceUtils.intToHexString(ServiceUtils.calculatePathHash("test", "23547", 0)));
        System.out.println("hexStringToInt(calculatePathHash('test','step1',0)) = "
                + ServiceUtils.intToHexString(step1));
        System.out.println("hexStringToInt(calculatePathHash('test','step2'," + step1 + ")) = "
                + ServiceUtils.intToHexString(step2));
        System.out.println("hexStringToInt(calculatePathHash('test','step3'," + step2 + ")) = "
                + ServiceUtils.intToHexString(step3));
        System.out.println("hexStringToInt(calculatePathHash('test','step4'," + step3 + ")) = "
                + ServiceUtils.intToHexString(step4));

        Assert.assertEquals("2e9a0b02", ServiceUtils.intToHexString(step1));
        Assert.assertEquals("01d3f0eb", ServiceUtils.intToHexString(step2));
        Assert.assertEquals("9a1b45e5", ServiceUtils.intToHexString(step3));
        Assert.assertEquals("e9eecfee", ServiceUtils.intToHexString(step4));

        Assert.assertEquals(step1, ServiceUtils.hexStringToInt(ServiceUtils.intToHexString(step1)));
        Assert.assertEquals(step2, ServiceUtils.hexStringToInt(ServiceUtils.intToHexString(step2)));
        Assert.assertEquals(step3, ServiceUtils.hexStringToInt(ServiceUtils.intToHexString(step3)));
        Assert.assertEquals(step4, ServiceUtils.hexStringToInt(ServiceUtils.intToHexString(step4)));

        // From external.py (test_catmap) AIT
        Assert.assertEquals("6e0e25c0", ServiceUtils.intToHexString(ServiceUtils.calculatePathHash(null,
                "WebTransaction/Servlet/ExternalCallServlet", ServiceUtils.hexStringToInt("715f4c55"))));
        Assert.assertEquals("590a4241", ServiceUtils.intToHexString(ServiceUtils.calculatePathHash(null,
                "WebTransaction/Servlet/TestServlet", ServiceUtils.hexStringToInt("6e0e25c0"))));
    }

    @Test
    public void md5HashValueFor_withEmptyString_returnsEmptyString() throws NoSuchAlgorithmException {
        assertEquals("", ServiceUtils.md5HashValueFor(""));
        assertEquals("", ServiceUtils.md5HashValueFor(null));
    }

    @Test
    public void md5HashValueFor_returnsCorrectMD5Hash() throws NoSuchAlgorithmException {
        assertEquals("a380157ccd4d4715187c72cbe34a2123", ServiceUtils.md5HashValueFor("SELECT * FROM FOO"));
    }
}
