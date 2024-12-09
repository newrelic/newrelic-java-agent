package io.asyncer.r2dbc.mysql.client;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import io.asyncer.r2dbc.mysql.ConnectionContext;
import io.asyncer.r2dbc.mysql.MySqlSslConfiguration;
import reactor.netty.Connection;

import java.net.SocketAddress;

@Weave(type = MatchType.ExactClass, originalName = "io.asyncer.r2dbc.mysql.client.ReactorNettyClient")
class ReactorNettyClient_Instrumentation {
    @NewField
    public final SocketAddress remoteAddress;

    ReactorNettyClient_Instrumentation(Connection connection, MySqlSslConfiguration ssl, ConnectionContext context) {
        this.remoteAddress = connection == null ? null :
                                connection.channel() == null ? null : connection.channel().remoteAddress();
    }
}
