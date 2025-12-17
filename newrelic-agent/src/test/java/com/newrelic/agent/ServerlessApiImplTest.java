/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.ServerlessApi;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ServerlessApiImplTest {

    private ServerlessApi serverlessApi;

    @Before
    public void setup() {
        serverlessApi = new ServerlessApiImpl();
    }

    @Test
    public void setAndGetArn() {
        String arn = "arn:aws:lambda:us-east-1:123456789012:function:my-function";
        serverlessApi.setArn(arn);
        assertEquals(arn, serverlessApi.getArn());
    }

    @Test
    public void setAndGetFunctionVersion() {
        String version = "$LATEST";
        serverlessApi.setFunctionVersion(version);
        assertEquals(version, serverlessApi.getFunctionVersion());
    }

    @Test
    public void getArn_whenNotSet_returnsNull() {
        assertNull(serverlessApi.getArn());
    }

    @Test
    public void getFunctionVersion_whenNotSet_returnsNull() {
        assertNull(serverlessApi.getFunctionVersion());
    }

    @Test
    public void setArn_withNull_doesNotStore() {
        serverlessApi.setArn("initial-arn");
        serverlessApi.setArn(null);
        assertEquals("initial-arn", serverlessApi.getArn());
    }

    @Test
    public void setArn_withEmptyString_doesNotStore() {
        serverlessApi.setArn("initial-arn");
        serverlessApi.setArn("");
        assertEquals("initial-arn", serverlessApi.getArn());
    }

    @Test
    public void setFunctionVersion_withNull_doesNotStore() {
        serverlessApi.setFunctionVersion("initial-version");
        serverlessApi.setFunctionVersion(null);
        assertEquals("initial-version", serverlessApi.getFunctionVersion());
    }

    @Test
    public void setFunctionVersion_withEmptyString_doesNotStore() {
        serverlessApi.setFunctionVersion("initial-version");
        serverlessApi.setFunctionVersion("");
        assertEquals("initial-version", serverlessApi.getFunctionVersion());
    }

    @Test
    public void setArn_canOverwritePreviousValue() {
        serverlessApi.setArn("first-arn");
        serverlessApi.setArn("second-arn");
        assertEquals("second-arn", serverlessApi.getArn());
    }

    @Test
    public void setFunctionVersion_canOverwritePreviousValue() {
        serverlessApi.setFunctionVersion("v1");
        serverlessApi.setFunctionVersion("v2");
        assertEquals("v2", serverlessApi.getFunctionVersion());
    }
}
