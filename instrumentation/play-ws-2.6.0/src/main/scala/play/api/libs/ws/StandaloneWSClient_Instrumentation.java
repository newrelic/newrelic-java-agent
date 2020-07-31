/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.api.libs.ws;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.playws.OutboundWrapper;
import com.nr.agent.instrumentation.playws.PlayWSUtils;
import scala.Tuple2;
import scala.collection.mutable.Buffer;

@Weave(type = MatchType.Interface, originalName = "play.api.libs.ws.StandaloneWSClient")
public class StandaloneWSClient_Instrumentation {

    public StandaloneWSRequest_Instrumentation url(String url) {
        Segment segment = PlayWSUtils.start();

        StandaloneWSRequest_Instrumentation request = Weaver.callOriginal();

        Buffer<Tuple2<String, String>> headers = PlayWSUtils.createSeq();
        segment.addOutboundRequestHeaders(new OutboundWrapper(headers));
        request.segment = segment;

        // This is here to ensure that we always add our outbound CAT headers
        return request.addHttpHeaders(headers);
    }

}
