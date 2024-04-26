package io.r2dbc.mssql.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.OperationAndTableName;
import com.newrelic.agent.bridge.datastore.R2dbcObfuscator;
import com.newrelic.agent.bridge.datastore.R2dbcOperation;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;
import io.r2dbc.mssql.MssqlResult;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;

import java.net.InetSocketAddress;

import java.util.function.Consumer;
import java.util.logging.Level;

public class R2dbcUtils {
    public static Flux<MssqlResult> wrapRequest(Flux<MssqlResult> request, String sql, Client client) {
        long start = System.currentTimeMillis();
        if(request != null) {
            Transaction transaction = NewRelic.getAgent().getTransaction();
            if(transaction != null && !(transaction instanceof NoOpTransaction)) {
                Segment segment = transaction.startSegment("execute");
                return request
                        .doOnSubscribe(reportExecution(sql, client, segment))
                        .doFinally((type) -> segment.end());
            }
        }
        long wrapperTime = System.currentTimeMillis() - start;
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "NR-262136: wrapping request took " + wrapperTime);
        return request;
    }

    private static Consumer<Subscription> reportExecution(String sql, Client client, Segment segment) {
        return (subscription) -> {
            long start = System.currentTimeMillis();
            OperationAndTableName sqlOperation = R2dbcOperation.extractFrom(sql);
            long sqlOpTime = System.currentTimeMillis() - start;
            String opName = (sqlOperation != null) ? sqlOperation.getOperation() : "none";
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "NR-262136: extracting SQL operation " + opName + " took " + sqlOpTime);
            InetSocketAddress socketAddress = extractSocketAddress(client);
            if (sqlOperation != null && socketAddress != null) {
                segment.reportAsExternal(DatastoreParameters
                        .product(DatastoreVendor.MSSQL.name())
                        .collection(sqlOperation.getTableName())
                        .operation(sqlOperation.getOperation())
                        .instance(socketAddress.getHostName(), socketAddress.getPort())
                        .databaseName(null)
                        .slowQuery(sql, R2dbcObfuscator.QUERY_CONVERTER)
                        .build());
            }
        };
    }

    public static InetSocketAddress extractSocketAddress(Client client) {
        try {
            if(client instanceof ReactorNettyClient_Instrumentation) {
                ReactorNettyClient_Instrumentation instrumentedClient = (ReactorNettyClient_Instrumentation) client;
                Connection clientConnection = instrumentedClient.clientConnection;
                    if(clientConnection.channel().remoteAddress() != null && clientConnection.channel().remoteAddress() instanceof InetSocketAddress) {
                        return (InetSocketAddress) clientConnection.channel().remoteAddress();
                    }
            }
            return null;
        } catch(Exception exception) {
            return null;
        }
    }
}
