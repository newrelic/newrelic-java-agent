/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.hc.core5.http.nio.support;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;

@Weave(type=MatchType.BaseClass, originalName = "org.apache.hc.core5.http.nio.support.BasicRequestProducer")
public class BasicRequestProducer_Instrumentation {

    @NewField
    public HttpRequest nrRequest;

    public BasicRequestProducer_Instrumentation(final HttpRequest request, final AsyncEntityProducer dataProducer) {
        nrRequest = request;
    }
}
