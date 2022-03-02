/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package redis.clients.jedis;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@SuppressWarnings({ "ResultOfMethodCallIgnored", "unused" })
@Weave(type = MatchType.BaseClass, originalName = "redis.clients.jedis.JedisPubSub")
public class JedisPubSub_Instrumentation {

    private volatile Connection client;

    @Trace
    public void onMessage(String channel, String message) {
        Weaver.callOriginal();

        reportMethodAsExternal("message");
    }

    @Trace
    public void onPMessage(String pattern, String channel, String message) {
        Weaver.callOriginal();

        reportMethodAsExternal("message");
    }

    @Trace
    public void onSubscribe(String channel, int subscribedChannels) {
        Weaver.callOriginal();

        reportMethodAsExternal("subscribe");
    }

    @Trace
    public void onUnsubscribe(String channel, int subscribedChannels) {
        Weaver.callOriginal();

        reportMethodAsExternal("unsubscribe");
    }

    @Trace
    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        Weaver.callOriginal();

        reportMethodAsExternal("unsubscribe");
    }

    @Trace
    public void onPSubscribe(String pattern, int subscribedChannels) {
        Weaver.callOriginal();

        reportMethodAsExternal("subscribe");
    }

    private void reportMethodAsExternal(String commandName) {
        NewRelic.getAgent().getTracedMethod().reportAsExternal(DatastoreParameters
                .product(DatastoreVendor.Redis.name())
                .collection(null)
                .operation(commandName)
                .instance(client.getHostAndPort().getHost(), client.getHostAndPort().getPort())
                .build());
    }
}
