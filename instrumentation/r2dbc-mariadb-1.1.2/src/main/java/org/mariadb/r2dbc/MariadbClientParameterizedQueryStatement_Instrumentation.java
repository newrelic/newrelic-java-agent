package org.mariadb.r2dbc;

import org.mariadb.r2dbc.api.MariadbResult;
import org.mariadb.r2dbc.client.Client;
import org.mariadb.r2dbc.message.Protocol;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.r2dbc.CancelHandler;
import com.nr.agent.instrumentation.r2dbc.NRHolder;
import com.nr.agent.instrumentation.r2dbc.R2dbcUtils;

import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "org.mariadb.r2dbc.MariadbClientParameterizedQueryStatement")
abstract class MariadbClientParameterizedQueryStatement_Instrumentation extends MariadbCommonStatement {
    
	MariadbClientParameterizedQueryStatement_Instrumentation(
		      Client client, String sql, MariadbConnectionConfiguration configuration) {
		    super(client, sql, configuration, Protocol.TEXT);
	 }

	@Trace(dispatcher = true)
    public Flux<MariadbResult> execute() {
        Flux<MariadbResult> request = Weaver.callOriginal();
        DatastoreParameters params = R2dbcUtils.getParams(initialSql, client);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("execute");
        NRHolder holder = new NRHolder(segment, params);
        return request.doOnError(holder).doOnTerminate(holder).doOnCancel(new CancelHandler(holder));
    }
}
