package io.r2dbc.mssql.client;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import reactor.netty.Connection;

@Weave(type = MatchType.ExactClass, originalName = "io.r2dbc.mssql.client.ReactorNettyClient")
public class ReactorNettyClient_Instrumentation {
    @NewField
    public final Connection clientConnection;

    private ReactorNettyClient_Instrumentation(Connection connection, TdsEncoder TdsEncoder, ConnectionContext context) {
        this.clientConnection = connection;
    }
}
