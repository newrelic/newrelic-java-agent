/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.http.nio.client;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;

import java.util.List;
import java.util.concurrent.Future;

// Weave this class to prevent instrumentation from applying to 4.0.x
@Weave(type = MatchType.Interface, originalName = "org.apache.http.nio.client.HttpPipeliningClient")
public class HttpPipeliningClient_Instrumentation {
    // There is some optimization code in the agent that expects that weaved classes have at least one method to match
    // against to help it from having to look at every single class and can instead just look at method signatures.
    public Future<List<HttpResponse>> execute(HttpHost target, List<HttpRequest> requests,
                                              FutureCallback<List<HttpResponse>> callback) {
        return Weaver.callOriginal();
    }
}