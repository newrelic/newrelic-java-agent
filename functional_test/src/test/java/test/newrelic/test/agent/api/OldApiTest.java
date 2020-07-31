/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.api;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class OldApiTest {

    @Test
    public void testResponse() throws Exception {
        Method method = v1Response.class.getMethod("getContentType");
        // verify that the getContentType method is added to v1Response as it loads
        Assert.assertNotNull(method);
        Assert.assertFalse(Modifier.isAbstract(method.getModifiers()));
    }

    private abstract static class v1Response implements Response {

        @Override
        public int getStatus() throws Exception {
            return 0;
        }

        @Override
        public String getStatusMessage() throws Exception {
            return null;
        }

        @Override
        public void setHeader(String name, String value) {

        }

        @Override
        public HeaderType getHeaderType() {
            return null;
        }
    }
}
