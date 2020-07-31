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
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.BaseClass)
public abstract class JedisPubSub {

    @NewField
    private String host;

    @NewField
    private int port;

    private void process(Client client) {
        this.host = client.getHost();
        this.port = client.getPort();
        Weaver.callOriginal();
    }

    @Trace
    public void onMessage(String channel, String message) {
        try {
            Weaver.callOriginal();
        } finally {
            instrument(NewRelic.getAgent().getTracedMethod(), "message", host, port);
        }
    }

    @Trace
    public void onPMessage(String pattern, String channel, String message) {
        try {
            Weaver.callOriginal();
        } finally {
            instrument(NewRelic.getAgent().getTracedMethod(), "message", host, port);
        }
    }

    @Trace
    public void onSubscribe(String channel, int subscribedChannels) {
        try {
            Weaver.callOriginal();
        } finally {
            instrument(NewRelic.getAgent().getTracedMethod(), "subscribe", host, port);
        }
    }

    @Trace
    public void onUnsubscribe(String channel, int subscribedChannels) {
        try {
            Weaver.callOriginal();
        } finally {
            instrument(NewRelic.getAgent().getTracedMethod(), "unsubscribe", host, port);
        }
    }

    @Trace
    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        try {
            Weaver.callOriginal();
        } finally {
            instrument(NewRelic.getAgent().getTracedMethod(), "unsubscribe", host, port);
        }
    }

    @Trace
    public void onPSubscribe(String pattern, int subscribedChannels) {
        try {
            Weaver.callOriginal();
        } finally {
            instrument(NewRelic.getAgent().getTracedMethod(), "subscribe", host, port);
        }
    }

    private void instrument(TracedMethod method, String commandName, String host, int port) {
        NewRelic.getAgent().getTracedMethod().reportAsExternal(DatastoreParameters
                .product(DatastoreVendor.Redis.name())
                .collection(null)
                .operation(commandName)
                .instance(host, port)
                .build());
    }
}
