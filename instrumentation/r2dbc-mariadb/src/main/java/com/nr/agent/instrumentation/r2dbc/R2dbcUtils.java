package com.nr.agent.instrumentation.r2dbc;

import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.datastore.R2dbcObfuscator;
import com.newrelic.agent.bridge.datastore.R2dbcOperation;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;
import org.mariadb.r2dbc.api.MariadbResult;
import org.mariadb.r2dbc.client.Client;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public class R2dbcUtils {
    public static Flux<MariadbResult> wrapRequest(Flux<MariadbResult> request, String sql, Client client) {
        if(request != null) {
            Transaction transaction = NewRelic.getAgent().getTransaction();
            if(transaction != null && !(transaction instanceof NoOpTransaction)) {
                Segment segment = transaction.startSegment("execute");
                return request
                        .doOnSubscribe(reportExecution(sql, client, segment))
                        .doFinally((type) -> segment.end());
            }
        }
        return request;
    }

    private static Consumer<Subscription> reportExecution(String sql, Client client, Segment segment) {
        return (subscription) -> {
            String[] sqlOperationCollection = R2dbcOperation.extractFrom(sql);
            if (sqlOperationCollection != null) {
                segment.reportAsExternal(DatastoreParameters
                        .product("MariaDB")
                        .collection(sqlOperationCollection[1])
                        .operation(sqlOperationCollection[0])
                        .instance(client.getConf().getHost(), client.getConf().getPort())
                        .databaseName(client.getConf().getDatabase())
                        .slowQuery(sql, R2dbcObfuscator.MYSQL_QUERY_CONVERTER)
                        .build());
            }
        };
    }
}
