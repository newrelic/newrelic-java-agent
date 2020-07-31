/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.sns;

import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.SNSInstrumentationHelper;

@Weave(type = MatchType.Interface, originalName = "com.amazonaws.services.sns.AmazonSNS")
public class AmazonSNS_Instrumentation {

    @Trace
    public PublishResult publish(PublishRequest publishRequest) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        ExternalParameters params = SNSInstrumentationHelper.makeMessageProducerParameters(publishRequest);
        tracedMethod.reportAsExternal(params);

        return Weaver.callOriginal();
    }
}
