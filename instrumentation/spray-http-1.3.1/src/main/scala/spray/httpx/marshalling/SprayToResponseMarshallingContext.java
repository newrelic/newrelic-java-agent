
/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.httpx.marshalling;

import com.agent.instrumentation.spray.OutboundWrapper;
import com.agent.instrumentation.spray.ResponseWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.collection.JavaConversions;
import spray.http.HttpHeader;
import spray.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

@Weave(type = MatchType.Interface, originalName = "spray.httpx.marshalling.ToResponseMarshallingContext")
public class SprayToResponseMarshallingContext {

    @Trace(async = true)
    public void marshalTo(HttpResponse httpResponse) {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            // First, convert to a java collection so we can add our CAT headers
            List<HttpHeader> modifiableHeaders = new ArrayList<>(
                    JavaConversions.asJavaCollection(httpResponse.headers()));

            // Run through CAT related code to set headers
            transaction.setWebResponse(new ResponseWrapper(httpResponse.status(), modifiableHeaders));
            OutboundWrapper outboundWrapper = new OutboundWrapper(modifiableHeaders);
            transaction.getCrossProcessState().processOutboundResponseHeaders(outboundWrapper,
                    getContentLength(outboundWrapper));
            transaction.markFirstByteOfResponse();

            httpResponse = httpResponse.withHeaders(JavaConversions.asScalaBuffer(modifiableHeaders).toList());
        }

        Weaver.callOriginal();
    }

    private long getContentLength(OutboundWrapper outboundWrapper) {
        String contentLength = outboundWrapper.getHeader("Content-Length");
        if (contentLength == null) {
            return -1L;
        } else {
            return Long.valueOf(contentLength);
        }
    }
}
