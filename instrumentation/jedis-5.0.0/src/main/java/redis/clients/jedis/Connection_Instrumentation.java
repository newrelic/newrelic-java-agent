/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
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

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@SuppressWarnings({ "ResultOfMethodCallIgnored", "WeakerAccess", "unused" }) // Weaver.callOriginal(), matching signatures
@Weave(type = MatchType.BaseClass, originalName = "redis.clients.jedis.Connection")
public abstract class Connection_Instrumentation {

    @NewField
    private long db = 0;

    abstract HostAndPort getHostAndPort();

    public void disconnect() {
        db = 0;
        Weaver.callOriginal();
    }

    @Trace
    public void sendCommand(final ProtocolCommand cmd, final byte[]... args) {
        Weaver.callOriginal();
        if (args != null && args.length > 0) {
            updateDbIndex(cmd, new String(args[0], StandardCharsets.UTF_8));
        }
    }

    @Trace
    public void sendCommand(final ProtocolCommand cmd, final String... args) {
        Weaver.callOriginal();
        if (args != null && args.length > 0) {
            updateDbIndex(cmd, args[0]);
        }
    }

    @Trace(leaf = true)
    public void sendCommand(final CommandArguments args) {
        Weaver.callOriginal();

        ProtocolCommand cmd = args.getCommand();
        reportMethodAsExternal(cmd);

    }

    private void updateDbIndex(ProtocolCommand cmd, String arg0) {
        try {
            if (cmd == Protocol.Command.SELECT) {
                db = Long.parseLong(arg0);
            }
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to set DB index");
        }
    }

    private void reportMethodAsExternal(ProtocolCommand command) {
        String operation = "unknown";
        try {

            operation = new String(command.getRaw(), Protocol.CHARSET).toLowerCase();
        } catch (Exception ignored) {
        }

        NewRelic.getAgent().getTracedMethod().reportAsExternal(DatastoreParameters
                .product(DatastoreVendor.Redis.name())
                .collection(null)
                .operation(operation)
                .instance(getHostAndPort().getHost(), getHostAndPort().getPort())
                .databaseName(String.valueOf(db))
                .build());
    }

}
