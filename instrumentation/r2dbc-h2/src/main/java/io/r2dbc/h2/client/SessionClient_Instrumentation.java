package io.r2dbc.h2.client;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import org.h2.engine.ConnectionInfo;

@Weave(type = MatchType.ExactClass, originalName = "io.r2dbc.h2.client.SessionClient")
public class SessionClient_Instrumentation {
    @NewField
    public final String databaseName;

    @NewField
    public final String url;

    public SessionClient_Instrumentation(ConnectionInfo connectionInfo, boolean shutdownDatabaseOnClose) {
        databaseName = connectionInfo.getName();
        url = connectionInfo.getURL();
    }
}
