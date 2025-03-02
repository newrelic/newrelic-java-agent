package org.mariadb.r2dbc;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.r2dbc.CancelHandler;
import com.nr.agent.instrumentation.r2dbc.NRHolder;
import com.nr.agent.instrumentation.r2dbc.R2dbcUtils;
import org.mariadb.r2dbc.api.MariadbResult;
import org.mariadb.r2dbc.client.Client;
import org.mariadb.r2dbc.message.Protocol;
import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "org.mariadb.r2dbc.MariadbServerParameterizedQueryStatement")
abstract class MariadbServerParameterizedQueryStatement_Instrumentation extends MariadbCommonStatement {

    MariadbServerParameterizedQueryStatement_Instrumentation(
            Client client, String sql, MariadbConnectionConfiguration configuration) {
        super(client, sql, configuration, Protocol.BINARY);
    }

    public Flux<MariadbResult> execute() {
        Flux<MariadbResult> request = Weaver.callOriginal();
        DatastoreParameters params = R2dbcUtils.getParams(initialSql, client);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("execute");
        NRHolder holder = new NRHolder(segment, params);
        return request.doOnError(holder).doOnTerminate(holder).doOnCancel(new CancelHandler(holder));
    }
}
