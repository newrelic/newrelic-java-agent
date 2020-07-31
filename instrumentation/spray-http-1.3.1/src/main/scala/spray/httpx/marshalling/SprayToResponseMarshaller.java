/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.httpx.marshalling;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "spray.httpx.marshalling.ToResponseMarshaller")
public class SprayToResponseMarshaller<T> {

    public void apply(final T obj, final ToResponseMarshallingContext ctx) {
        Weaver.callOriginal();
    }

}
