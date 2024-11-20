package com.nr.agent.instrumentation.r2dbc;

import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.datastore.OperationAndTableName;
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
        if (request != null) {
            Transaction transaction = NewRelic.getAgent().getTransaction();
            if (transaction != null && !(transaction instanceof NoOpTransaction)) {
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
            OperationAndTableName sqlOperation = R2dbcOperation.extractFrom(sql);
            if (sqlOperation != null) {
                segment.reportAsExternal(DatastoreParameters
                        .product("MariaDB")
                        .collection(sqlOperation.getTableName())
                        .operation(sqlOperation.getOperation())
                        .instance(client.getHostAddress().getHost(), client.getHostAddress().getPort())
                        .databaseName(client.getContext().getDatabase())
                        .slowQuery(sql, R2dbcObfuscator.MYSQL_QUERY_CONVERTER)
                        .build());
            }
        };
    }

    public static DatastoreParameters getParams(String sql, Client client) {
        OperationAndTableName sqlOperation = R2dbcOperation.extractFrom(sql);
        if (sqlOperation == null) {
            return null;
        }
        return DatastoreParameters
                .product("MariaDB")
                .collection(sqlOperation.getTableName())
                .operation(sqlOperation.getOperation())
                .instance(client.getHostAddress().getHost(), client.getHostAddress().getPort())
                .databaseName(client.getContext().getDatabase())
                .slowQuery(sql, R2dbcObfuscator.MYSQL_QUERY_CONVERTER)
                .build();
    }

}
