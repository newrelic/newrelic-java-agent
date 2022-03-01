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
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@SuppressWarnings({ "ResultOfMethodCallIgnored", "unused" })
@Weave(type = MatchType.BaseClass, originalName = "redis.clients.jedis.JedisPubSub")
public class JedisPubSub_Instrumentation {

    @NewField
    private String host;

    @NewField
    private int port;

    @Trace
    private void process() {
        Weaver.callOriginal();
    }

    public void proceed(Connection client, String... channels) {
        this.host = client.getHostAndPort().getHost();
        this.port = client.getHostAndPort().getPort();
        Weaver.callOriginal();
    }

    @Trace
    public void onMessage(String channel, String message) {
        Weaver.callOriginal();

        reportMethodAsExternal("message", host, port);
    }

    @Trace
    public void onPMessage(String pattern, String channel, String message) {
        Weaver.callOriginal();

        reportMethodAsExternal("message", host, port);
    }

    @Trace
    public void onSubscribe(String channel, int subscribedChannels) {
        Weaver.callOriginal();

        reportMethodAsExternal("subscribe", host, port);
    }

    @Trace
    public void onUnsubscribe(String channel, int subscribedChannels) {
        Weaver.callOriginal();

        reportMethodAsExternal("unsubscribe", host, port);
    }

    @Trace
    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        Weaver.callOriginal();

        reportMethodAsExternal("unsubscribe", host, port);
    }

    @Trace
    public void onPSubscribe(String pattern, int subscribedChannels) {
        Weaver.callOriginal();

        reportMethodAsExternal("subscribe", host, port);
    }

    private void reportMethodAsExternal(String commandName, String host, int port) {
        NewRelic.getAgent().getTracedMethod().reportAsExternal(DatastoreParameters
                .product(DatastoreVendor.Redis.name())
                .collection(null)
                .operation(commandName)
                .instance(host, port)
                .build());
    }
}
