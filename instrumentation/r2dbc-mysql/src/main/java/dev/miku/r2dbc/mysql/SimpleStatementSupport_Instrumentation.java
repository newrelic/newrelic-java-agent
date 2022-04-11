package dev.miku.r2dbc.mysql;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import dev.miku.r2dbc.mysql.client.Client;

@Weave(type = MatchType.ExactClass, originalName = "dev.miku.r2dbc.mysql.SimpleStatementSupport")
abstract class SimpleStatementSupport_Instrumentation {
    protected final Client client = Weaver.callOriginal();
    protected final String sql = Weaver.callOriginal();
}
