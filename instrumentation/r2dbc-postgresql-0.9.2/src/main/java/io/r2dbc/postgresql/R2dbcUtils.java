/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.r2dbc.postgresql;

import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.OperationAndTableName;
import com.newrelic.agent.bridge.datastore.R2dbcObfuscator;
import com.newrelic.agent.bridge.datastore.R2dbcOperation;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;
import com.nr.instrumentation.r2dbc.postgresql092.EndpointData;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.postgresql.client.Client;
import io.r2dbc.postgresql.client.ReactorNettyClient_Instrumentation;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.util.annotation.Nullable;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class R2dbcUtils {
    public static Flux<PostgresqlResult> wrapRequest(Flux<PostgresqlResult> request, String sql, Client client, PostgresqlConnectionConfiguration connectionConfiguration) {
        if(request != null) {
            Transaction transaction = NewRelic.getAgent().getTransaction();
            EndpointData endpointData = extractEndpointData(client);
            if(transaction != null && !(transaction instanceof NoOpTransaction) && endpointData != null) {
                Segment segment = transaction.startSegment("execute");
                return request
                        .doOnSubscribe(reportExecution(sql, endpointData, connectionConfiguration, segment))
                        .doFinally((type) -> segment.end());
            }
        }
        return request;
    }

    private static Consumer<Subscription> reportExecution(String sql, EndpointData endpointData, PostgresqlConnectionConfiguration connectionConfiguration, Segment segment) {
        return (subscription) -> {
            OperationAndTableName sqlOperation = R2dbcOperation.extractFrom(sql);
            if (sqlOperation != null) {
                segment.reportAsExternal(DatastoreParameters
                        .product(DatastoreVendor.Postgres.name())
                        .collection(sqlOperation.getTableName())
                        .operation(sqlOperation.getOperation())
                        .instance(endpointData.getHostName(), endpointData.getPort())
                        .databaseName(connectionConfiguration.getDatabase())
                        .slowQuery(sql, R2dbcObfuscator.POSTGRES_QUERY_CONVERTER)
                        .build());
            }
        };
    }

    public static @Nullable EndpointData extractEndpointData(Client client) {
        try {
            if(client instanceof ReactorNettyClient_Instrumentation) {
                ReactorNettyClient_Instrumentation instrumentedClient = (ReactorNettyClient_Instrumentation) client;
                return instrumentedClient.endpointData;
            }
            return null;
        } catch(Exception exception) {
            return null;
        }
    }
}
