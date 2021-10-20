/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.api.agent.Trace;
import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java12IncompatibleTest;
import com.newrelic.test.marker.Java13IncompatibleTest;
import com.newrelic.test.marker.Java14IncompatibleTest;
import com.newrelic.test.marker.Java15IncompatibleTest;
import com.newrelic.test.marker.Java16IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import javax.activation.MimeType;

@Category({ Java11IncompatibleTest.class, Java12IncompatibleTest.class, Java13IncompatibleTest.class,
        Java14IncompatibleTest.class, Java15IncompatibleTest.class, Java16IncompatibleTest.class,
        Java17IncompatibleTest.class })
public class CustomExtensionTestAddition {

    @Test
    @Trace(dispatcher = true)
    public void testRecordObjectParam() throws Exception {
        PublicApi original = AgentBridge.publicApi;
        PublicApi api = Mockito.mock(PublicApi.class);
        AgentBridge.publicApi = api;
        try {
            MimeType sample = new MimeType();
            sample.getParameter("recordThis");
            Mockito.verify(api).addCustomParameter("theKey", "recordThis");
        } finally {
            AgentBridge.publicApi = original;
        }
    }

}
