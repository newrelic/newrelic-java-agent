package io.asyncer.r2dbc.mysql;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.asyncer.r2dbc.mysql.client.Client;

@Weave(type = MatchType.ExactClass, originalName = "io.asyncer.r2dbc.mysql.MySqlStatementSupport")
abstract class MySqlStatementSupport_Instrumentation {
    protected final Client client = Weaver.callOriginal();
}