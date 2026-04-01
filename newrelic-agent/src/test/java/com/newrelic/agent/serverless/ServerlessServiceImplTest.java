/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.serverless;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ServerlessServiceImplTest {

    private ServerlessService serverlessService;

    @Before
    public void setup() {
        serverlessService = new ServerlessServiceImpl();
    }

    @Test
    public void setAndGetArn() {
        String arn = "arn:aws:lambda:us-east-1:123456789012:function:my-function";
        serverlessService.setArn(arn);
        assertEquals(arn, serverlessService.getArn());
    }

    @Test
    public void setAndGetFunctionVersion() {
        String version = "$LATEST";
        serverlessService.setFunctionVersion(version);
        assertEquals(version, serverlessService.getFunctionVersion());
    }

    @Test
    public void getArn_whenNotSet_returnsNull() {
        assertNull(serverlessService.getArn());
    }

    @Test
    public void getFunctionVersion_whenNotSet_returnsNull() {
        assertNull(serverlessService.getFunctionVersion());
    }

    @Test
    public void setArn_withNull_doesNotStore() {
        serverlessService.setArn("initial-arn");
        serverlessService.setArn(null);
        assertEquals("initial-arn", serverlessService.getArn());
    }

    @Test
    public void setArn_withEmptyString_doesNotStore() {
        serverlessService.setArn("initial-arn");
        serverlessService.setArn("");
        assertEquals("initial-arn", serverlessService.getArn());
    }

    @Test
    public void setFunctionVersion_withNull_doesNotStore() {
        serverlessService.setFunctionVersion("initial-version");
        serverlessService.setFunctionVersion(null);
        assertEquals("initial-version", serverlessService.getFunctionVersion());
    }

    @Test
    public void setFunctionVersion_withEmptyString_doesNotStore() {
        serverlessService.setFunctionVersion("initial-version");
        serverlessService.setFunctionVersion("");
        assertEquals("initial-version", serverlessService.getFunctionVersion());
    }

    @Test
    public void setArn_canOverwritePreviousValue() {
        serverlessService.setArn("first-arn");
        serverlessService.setArn("second-arn");
        assertEquals("second-arn", serverlessService.getArn());
    }

    @Test
    public void setFunctionVersion_canOverwritePreviousValue() {
        serverlessService.setFunctionVersion("v1");
        serverlessService.setFunctionVersion("v2");
        assertEquals("v2", serverlessService.getFunctionVersion());
    }
}
