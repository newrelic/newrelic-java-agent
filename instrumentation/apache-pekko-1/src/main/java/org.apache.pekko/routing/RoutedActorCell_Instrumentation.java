/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.routing;

import org.apache.pekko.dispatch.Envelope_Instrumentation;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.ExactClass, originalName = "org.apache.pekko.routing.RoutedActorCell")
public abstract class RoutedActorCell_Instrumentation {

    @Trace
    public void sendMessage(Envelope_Instrumentation envelope) {
        Weaver.callOriginal();

        // The "RoutedActorCell" does not act like other actors, we need to expire the token on the envelope to prevent a token timeout
        if (envelope.token != null) {
            envelope.token.expire();
            envelope.token = null;
        }
    }

}
