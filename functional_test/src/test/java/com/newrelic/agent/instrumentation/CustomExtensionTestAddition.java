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
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.newrelic.test.marker.Java21IncompatibleTest;
import com.newrelic.test.marker.Java23IncompatibleTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import javax.activation.MimeType;

@Category({ Java11IncompatibleTest.class, Java17IncompatibleTest.class, Java21IncompatibleTest.class, Java23IncompatibleTest.class })
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
