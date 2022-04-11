package dev.miku.r2dbc.mysql.client;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import dev.miku.r2dbc.mysql.ConnectionContext;
import dev.miku.r2dbc.mysql.MySqlSslConfiguration;
import reactor.netty.Connection;

@Weave(type = MatchType.ExactClass, originalName = "dev.miku.r2dbc.mysql.client.ReactorNettyClient")
class ReactorNettyClient_Instrumentation {
    @NewField
    public final Connection clientConnection;

    ReactorNettyClient_Instrumentation(Connection connection, MySqlSslConfiguration ssl, ConnectionContext context) {
        this.clientConnection = connection;
    }
}
