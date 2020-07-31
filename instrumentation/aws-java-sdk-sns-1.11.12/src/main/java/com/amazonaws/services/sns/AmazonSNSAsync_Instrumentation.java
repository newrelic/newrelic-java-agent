/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.sns;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.SNSInstrumentationHelper;

import java.util.concurrent.Future;

@Weave(type = MatchType.Interface, originalName = "com.amazonaws.services.sns.AmazonSNSAsync")
public class AmazonSNSAsync_Instrumentation {

    @Trace
    public Future<PublishResult> publishAsync(final PublishRequest request, AsyncHandler<PublishRequest, PublishResult> asyncHandler) {
        final AsyncHandler<PublishRequest, PublishResult> originalHandler = asyncHandler;
        final Transaction transaction = NewRelic.getAgent().getTransaction();
        final Segment segment = startSegment(request, transaction);

        //replace the handler with ours that wraps the original (which might be null)
        asyncHandler = new AsyncHandler<PublishRequest, PublishResult>() {
            @Override
            public void onError(Exception exception) {
                try {
                    if (originalHandler != null) {
                        originalHandler.onError(exception);
                    }
                } finally {
                    segment.end();
                }
            }

            @Override
            public void onSuccess(PublishRequest request, PublishResult publishResult) {
                try {
                    if (originalHandler != null) {
                        originalHandler.onSuccess(request, publishResult);
                    }
                } finally {
                    segment.end();
                }
            }
        };

        return Weaver.callOriginal();
    }

    private Segment startSegment(PublishRequest publishRequest, Transaction transaction) {
        final Segment segment = transaction.startSegment("SNS");
        MessageProduceParameters params = SNSInstrumentationHelper.makeMessageProducerParameters(publishRequest);
        segment.reportAsExternal(params);
        return segment;
    }

}
