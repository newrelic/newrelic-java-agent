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
import io.r2dbc.postgresql.client.MultiHostConfiguration;
import io.r2dbc.postgresql.client.SingleHostConfiguration;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.util.annotation.Nullable;

import java.util.List;
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
                ServerHost serverHost = getServerHost(connectionConfiguration);

                if (serverHost != null) {
                    segment.reportAsExternal(DatastoreParameters
                            .product(DatastoreVendor.Postgres.name())
                            .collection(sqlOperation.getTableName())
                            .operation(sqlOperation.getOperation())
                            .instance(serverHost.getHost(), serverHost.getPort())
                            .databaseName(connectionConfiguration.getDatabase())
                            .slowQuery(sql, R2dbcObfuscator.POSTGRES_QUERY_CONVERTER)
                            .build());
                }
            }
        };
    }

    private static @Nullable ServerHost getServerHost(PostgresqlConnectionConfiguration connectionConfiguration) {
        SingleHostConfiguration singleHostConfiguration = connectionConfiguration.getSingleHostConfiguration();
        MultiHostConfiguration multiHostConfiguration = connectionConfiguration.getMultiHostConfiguration();

        if (multiHostConfiguration != null) {
            List<MultiHostConfiguration.ServerHost> hosts = multiHostConfiguration.getHosts();

            return hosts.stream().findFirst()
                    .map(s -> new ServerHost(s.getHost(), s.getPort())).orElse(null);
        }

        if (singleHostConfiguration != null && singleHostConfiguration.getHost() != null) {
            return new ServerHost(singleHostConfiguration.getHost(), singleHostConfiguration.getPort());
        }

        return null;
    }

    private static class ServerHost {
        private final String host;
        private final int port;

        public ServerHost(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}
