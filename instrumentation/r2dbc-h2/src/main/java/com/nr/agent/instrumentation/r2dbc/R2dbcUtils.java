package com.nr.agent.instrumentation.r2dbc;

import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.agent.bridge.datastore.R2dbcObfuscator;
import com.newrelic.agent.bridge.datastore.R2dbcOperation;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;
import io.r2dbc.h2.H2Result;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.function.Consumer;

public class R2dbcUtils {
    public static Flux<H2Result> wrapRequest(Flux<H2Result> request, String sql, String databaseName, String url) {
        if(request != null) {
            Transaction transaction = NewRelic.getAgent().getTransaction();
            if(transaction != null && !(transaction instanceof NoOpTransaction)) {
                Segment segment = transaction.startSegment("execute");
                return request
                        .doOnSubscribe(onSubscription(sql, databaseName, url, segment))
                        .doFinally(onFinally(segment));
            }
        }
        return request;
    }

    private static Consumer<Subscription> onSubscription(String sql, String databaseName, String url, Segment segment) {
        return (subscription) -> {
            String[] sqlOperationCollection = R2dbcOperation.extractFrom(sql);
            if (sqlOperationCollection != null) {
                segment.reportAsExternal(DatastoreParameters
                        .product(DatastoreVendor.H2.name())
                        .collection(sqlOperationCollection[1])
                        .operation(sqlOperationCollection[0])
                        .instance("localhost", JdbcHelper.parseInMemoryIdentifier(url))
                        .databaseName(databaseName)
                        .slowQuery(sql, R2dbcObfuscator.R2DBC_QUERY_CONVERTER)
                        .build());
            }
        };
    }

    private static Consumer<SignalType> onFinally(Segment segment) {
        return (type) -> segment.end();
    }
}
