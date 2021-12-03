/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.libs.ws;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.playws.JavaOutboundWrapper;
import com.nr.agent.instrumentation.playws.JavaPlayWSUtils;

import java.util.HashMap;
import java.util.Map;

@Weave(type = MatchType.Interface, originalName = "play.libs.ws.StandaloneWSClient")
public class StandaloneWSClient_Instrumentation {

    public StandaloneWSRequest_Instrumentation url(String url) {
        Segment segment = JavaPlayWSUtils.start();

        StandaloneWSRequest_Instrumentation request = Weaver.callOriginal();

        Map<String, String> nrHeaders = new HashMap<>();
        segment.addOutboundRequestHeaders(new JavaOutboundWrapper(nrHeaders));
        request.segment = segment;

        // This is here to ensure that we always add our outbound CAT headers
        for (Map.Entry<String, String> entry : nrHeaders.entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
        return request;
    }

}
