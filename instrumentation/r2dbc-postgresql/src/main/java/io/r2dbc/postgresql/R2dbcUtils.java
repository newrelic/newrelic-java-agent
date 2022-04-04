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
import io.r2dbc.postgresql.api.PostgresqlResult;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public class R2dbcUtils {
    public static Flux<PostgresqlResult> wrapRequest(Flux<PostgresqlResult> request, String sql, PostgresqlConnectionConfiguration connectionConfiguration) {
        if(request != null) {
            Transaction transaction = NewRelic.getAgent().getTransaction();
            if(transaction != null && !(transaction instanceof NoOpTransaction)) {
                Segment segment = transaction.startSegment("execute");
                return request
                        .doOnSubscribe(reportExecution(sql, connectionConfiguration, segment))
                        .doFinally((type) -> segment.end());
            }
        }
        return request;
    }

    private static Consumer<Subscription> reportExecution(String sql, PostgresqlConnectionConfiguration connectionConfiguration, Segment segment) {
        return (subscription) -> {
            OperationAndTableName sqlOperation = R2dbcOperation.extractFrom(sql);
            if (sqlOperation != null) {
                segment.reportAsExternal(DatastoreParameters
                        .product(DatastoreVendor.Postgres.name())
                        .collection(sqlOperation.getTableName())
                        .operation(sqlOperation.getOperation())
                        .instance(connectionConfiguration.getHost(), connectionConfiguration.getPort())
                        .databaseName(connectionConfiguration.getDatabase())
                        .slowQuery(sql, R2dbcObfuscator.MYSQL_QUERY_CONVERTER)
                        .build());
            }
        };
    }
}
