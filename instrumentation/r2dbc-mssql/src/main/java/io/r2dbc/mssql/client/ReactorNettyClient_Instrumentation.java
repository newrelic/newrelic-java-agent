package io.r2dbc.mssql.client;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import reactor.netty.Connection;

import java.net.SocketAddress;

@Weave(type = MatchType.ExactClass, originalName = "io.r2dbc.mssql.client.ReactorNettyClient")
public class ReactorNettyClient_Instrumentation {
    @NewField
    public final SocketAddress remoteAddress;

    private ReactorNettyClient_Instrumentation(Connection connection, TdsEncoder TdsEncoder, ConnectionContext context) {
        this.remoteAddress = connection == null ? null :
                connection.channel() == null ? null : connection.channel().remoteAddress();
    }
}
