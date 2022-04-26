package oracle.r2dbc.impl;

import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.agent.bridge.datastore.OperationAndTableName;
import com.newrelic.agent.bridge.datastore.R2dbcObfuscator;
import com.newrelic.agent.bridge.datastore.R2dbcOperation;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.util.function.Consumer;

public class R2dbcUtils {
    public static Flux<OracleResultImpl> wrapRequest(Flux<OracleResultImpl> request, String sql, Connection jdbcConnection) {
        if(request != null) {
            Transaction transaction = NewRelic.getAgent().getTransaction();
            if(transaction != null && !(transaction instanceof NoOpTransaction)) {
                Segment segment = transaction.startSegment("execute");
                return request
                        .doOnSubscribe(reportExecution(sql, jdbcConnection, segment))
                        .doFinally((type) -> segment.end());
            }
        }
        return request;
    }

    private static Consumer<Subscription> reportExecution(String sql, Connection jdbcConnection, Segment segment) {
        return (subscription) -> {
            OperationAndTableName sqlOperation = R2dbcOperation.extractFrom(sql);
            InetSocketAddress socketAddress = DatastoreInstanceDetection.getAddressForConnection(jdbcConnection);
            String cachedIdentifier = JdbcHelper.getCachedIdentifierForConnection(jdbcConnection);
            String identifier = cachedIdentifier != null ? cachedIdentifier : JdbcHelper.parseAndCacheInMemoryIdentifier(jdbcConnection);
            if (sqlOperation != null && socketAddress != null && identifier != null) {
                segment.reportAsExternal(DatastoreParameters
                        .product(DatastoreVendor.Postgres.name())
                        .collection(sqlOperation.getTableName())
                        .operation(sqlOperation.getOperation())
                        .instance(socketAddress.getHostName(), socketAddress.getPort())
                        .databaseName(identifier)
                        .slowQuery(sql, R2dbcObfuscator.POSTGRES_QUERY_CONVERTER)
                        .build());
            }
        };
    }
}
