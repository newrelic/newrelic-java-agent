/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package redis.clients.jedis;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
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
public abstract class Connection {

    @NewField
    private long db = 0;

    public abstract String getHost();

    public abstract int getPort();

    public void disconnect() {
        db = 0;
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    protected Connection sendCommand(final ProtocolCommand cmd, final byte[]... args) {
        try {
            return Weaver.callOriginal();
        } catch (RuntimeException jedisConnectionException) {
            AgentBridge.privateApi.reportException(jedisConnectionException);
            throw jedisConnectionException;
        } finally {
            try {
                if (cmd == Protocol.Command.SELECT) {
                    db = Long.parseLong(new String(args[0], StandardCharsets.UTF_8));
                } else if (cmd == Protocol.Command.QUIT) {
                    db = 0;
                }
            } catch (Throwable t) {
                AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to set DB index");
            }

            instrument(NewRelic.getAgent().getTracedMethod(), cmd.getRaw(), getHost(), getPort());
        }
    }

    @Trace(leaf = true)
    protected Connection sendCommand(final Protocol.Command cmd) {
        try {
            return Weaver.callOriginal();
        } catch (RuntimeException jedisConnectionException) {
            AgentBridge.privateApi.reportException(jedisConnectionException);
            throw jedisConnectionException;
        } finally {
            instrument(NewRelic.getAgent().getTracedMethod(), cmd.getRaw(), getHost(), getPort());
        }
    }

    private void instrument(TracedMethod method, byte[] cmd, String host, int port) {
        NewRelic.getAgent().getTracedMethod().reportAsExternal(DatastoreParameters
                .product(DatastoreVendor.Redis.name())
                .collection(null)
                .operation(new String(cmd, StandardCharsets.UTF_8).toLowerCase())
                .instance(host, port)
                .databaseName(String.valueOf(db))
                .build());
    }

}
