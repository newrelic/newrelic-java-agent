/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.netty.channel;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.Channel_Instrumentation;
import reactor.core.CoreSubscriber;

@Weave(originalName = "reactor.netty.channel.FluxReceive")
abstract class FluxReceive_Instrumentation {

    final Channel_Instrumentation channel = Weaver.callOriginal();

    @Trace(async = true)
    public void subscribe(CoreSubscriber<? super Object> s) {
        if (channel.pipeline().reactiveLayerToken != null) {
            channel.pipeline().reactiveLayerToken.link();
        }
        Weaver.callOriginal();
    }
}
