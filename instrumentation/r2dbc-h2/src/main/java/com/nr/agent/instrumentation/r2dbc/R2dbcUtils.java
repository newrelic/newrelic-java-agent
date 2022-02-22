package com.nr.agent.instrumentation.r2dbc;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.R2dbcObfuscator;
import com.newrelic.agent.bridge.datastore.R2dbcOperation;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.Segment;
import io.r2dbc.h2.H2Result;
import io.r2dbc.h2.client.Client;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.util.NetworkConnectionInfo;
import reactor.core.publisher.Flux;

public class R2dbcUtils {
    public static Flux<H2Result> wrapRequest(Flux<H2Result> request, Client client, String sql, Segment segment) {

        return request != null ? request
                .doOnSubscribe((subscription) -> {
                    String[] sqlOperationCollection = R2dbcOperation.extractFrom(sql);
                    if(sqlOperationCollection != null) {
                        segment.reportAsExternal(DatastoreParameters
                                .product(DatastoreVendor.H2.name())
                                .collection(sqlOperationCollection[1])
                                .operation(sqlOperationCollection[0])
                                .instance(extractHost(client), extractIdentifier(client))
                                .databaseName(((Database) client.getSession().getDataHandler()).getName())
                                .slowQuery(sql, R2dbcObfuscator.R2DBC_QUERY_CONVERTER)
                                .build());
                    }
                })
                .doFinally((type) -> segment.end()) : null;

    }

    public static String extractHost(Client client) {
        try {
            Session session = (Session) client.getSession();
            NetworkConnectionInfo networkConnectionInfo = session.getNetworkConnectionInfo();
            return networkConnectionInfo.getClient();
        } catch (Exception exception) {
            return "unknowm";
        }
    }

    public static String extractIdentifier(Client client) {
        try {
            Session session = (Session) client.getSession();
            NetworkConnectionInfo networkConnectionInfo = session.getNetworkConnectionInfo();
            return String.valueOf(networkConnectionInfo.getClientPort());
        } catch (Exception exception) {
            return "unknown";
        }
    }
}
