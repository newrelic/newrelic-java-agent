/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package redis.clients.jedis;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import redis.clients.jedis.commands.ProtocolCommand;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@SuppressWarnings({ "ResultOfMethodCallIgnored", "WeakerAccess", "unused" }) // Weaver.callOriginal(), matching signatures
@Weave(type = MatchType.BaseClass, originalName = "redis.clients.jedis.Connection")
public abstract class Connection_Instrumentation {

    @NewField
    private long db = 0;

    public abstract String getHost();

    public abstract int getPort();

    public void disconnect() {
        db = 0;
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void sendCommand(final ProtocolCommand cmd, final byte[]... args) {
        Weaver.callOriginal();
        if (args != null && args.length > 0) {
            updateDbIndex(cmd, new String(args[0], StandardCharsets.UTF_8));
        }
        reportMethodAsExternal(cmd, getHost(), getPort());
    }

    @Trace(leaf = true)
    public void sendCommand(final ProtocolCommand cmd, final String... args) {
        Weaver.callOriginal();
        if (args != null && args.length > 0) {
            updateDbIndex(cmd, args[0]);
        }
        reportMethodAsExternal(cmd, getHost(), getPort());
    }

    @Trace(leaf = true)
    public void sendCommand(final ProtocolCommand cmd) {
        Weaver.callOriginal();

        reportMethodAsExternal(cmd, getHost(), getPort());
    }

    private void updateDbIndex(ProtocolCommand cmd, String arg0) {
        try {
            if (cmd == Protocol.Command.SELECT) {
                db = Long.parseLong(arg0);
            } else if (cmd == Protocol.Command.QUIT) {
                db = 0;
            }
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to set DB index");
        }
    }

    private void reportMethodAsExternal(ProtocolCommand command, String serverUsed, int serverPortUsed) {
        String operation = "unknown";
        try {
            operation = new String(command.getRaw(), Protocol.CHARSET).toLowerCase();
        } catch (UnsupportedEncodingException ignored) { }

        NewRelic.getAgent().getTracedMethod().reportAsExternal(DatastoreParameters
                .product(DatastoreVendor.Redis.name())
                .collection(null)
                .operation(operation)
                .instance(serverUsed, serverPortUsed)
                .databaseName(String.valueOf(db))
                .build());
    }

}
