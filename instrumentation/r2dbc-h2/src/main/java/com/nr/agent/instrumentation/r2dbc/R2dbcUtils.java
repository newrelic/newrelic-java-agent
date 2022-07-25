package com.nr.agent.instrumentation.r2dbc;

import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.agent.bridge.datastore.OperationAndTableName;
import com.newrelic.agent.bridge.datastore.R2dbcObfuscator;
import com.newrelic.agent.bridge.datastore.R2dbcOperation;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;
import io.r2dbc.h2.H2Result;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public class R2dbcUtils {
    public static Flux<H2Result> wrapRequest(Flux<H2Result> request, String sql, String databaseName, String url) {
        if(request != null) {
            Transaction transaction = NewRelic.getAgent().getTransaction();
            if(transaction != null && !(transaction instanceof NoOpTransaction)) {
                Segment segment = transaction.startSegment("execute");
                return request
                        .doOnSubscribe(reportExecution(sql, databaseName, url, segment))
                        .doFinally((type) -> segment.end());
            }
        }
        return request;
    }

    private static Consumer<Subscription> reportExecution(String sql, String databaseName, String url, Segment segment) {
        return (subscription) -> {
            OperationAndTableName sqlOperation = R2dbcOperation.extractFrom(sql);
            if (sqlOperation != null) {
                segment.reportAsExternal(DatastoreParameters
                        .product(DatastoreVendor.H2.name())
                        .collection(sqlOperation.getTableName())
                        .operation(sqlOperation.getOperation())
                        .instance("localhost", JdbcHelper.parseInMemoryIdentifier(url))
                        .databaseName(databaseName)
                        .slowQuery(sql, R2dbcObfuscator.QUERY_CONVERTER)
                        .build());
            }
        };
    }
}
