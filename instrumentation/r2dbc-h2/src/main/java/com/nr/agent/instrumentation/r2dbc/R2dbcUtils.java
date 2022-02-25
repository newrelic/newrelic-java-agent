package com.nr.agent.instrumentation.r2dbc;

import com.newrelic.agent.bridge.NoOpSegment;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.agent.bridge.datastore.R2dbcObfuscator;
import com.newrelic.agent.bridge.datastore.R2dbcOperation;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import io.r2dbc.h2.H2Result;
import reactor.core.publisher.Flux;

public class R2dbcUtils {
    public static Flux<H2Result> wrapRequest(Flux<H2Result> request, String sql, String databaseName, String url) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("execute");
        return request != null ? request
                .doOnSubscribe((subscription) -> {
                    if(!(segment instanceof NoOpSegment)) {
                        String[] sqlOperationCollection = R2dbcOperation.extractFrom(sql);
                        if(sqlOperationCollection != null) {
                            segment.reportAsExternal(DatastoreParameters
                                    .product(DatastoreVendor.H2.name())
                                    .collection(sqlOperationCollection[1])
                                    .operation(sqlOperationCollection[0])
                                    .instance("localhost", JdbcHelper.parseInMemoryIdentifier(url))
                                    .databaseName(databaseName)
                                    .slowQuery(sql, R2dbcObfuscator.R2DBC_QUERY_CONVERTER)
                                    .build());
                        }
                    }
                })
                .doFinally((type) -> segment.end()) : null;
    }
}
