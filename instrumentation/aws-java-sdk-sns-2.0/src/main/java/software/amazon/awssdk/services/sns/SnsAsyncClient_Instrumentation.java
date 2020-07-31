/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.sns;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static com.nr.agent.instrumentation.SnsClientInstrumentationHelper.startSegmentAndReportAsExternal;

@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.sns.SnsAsyncClient")
public class SnsAsyncClient_Instrumentation {

    @SuppressWarnings("Convert2Lambda")
    @Trace
    public CompletableFuture<PublishResponse> publish(PublishRequest publishRequest) {
        final Segment segment = startSegmentAndReportAsExternal(publishRequest);
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<PublishResponse> result = Weaver.callOriginal();
        if (result == null) {  // this should never happen, but protecting against bad implementations
            segment.end();
        } else {
            result.whenComplete(new BiConsumer<PublishResponse, Throwable>() {
                @Override
                public void accept(PublishResponse publishResponse, Throwable throwable) {
                    try {
                        segment.end();
                    } catch (Throwable t) {
                        AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
                    }
                }
            });
        }

        return result;
    }

}
