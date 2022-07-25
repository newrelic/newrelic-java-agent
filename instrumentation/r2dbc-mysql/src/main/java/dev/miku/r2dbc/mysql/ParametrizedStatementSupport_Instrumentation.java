package dev.miku.r2dbc.mysql;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import dev.miku.r2dbc.mysql.client.Client;

@Weave(type = MatchType.ExactClass, originalName = "dev.miku.r2dbc.mysql.ParametrizedStatementSupport")
abstract class ParametrizedStatementSupport_Instrumentation {
    protected final Client client = Weaver.callOriginal();
}
