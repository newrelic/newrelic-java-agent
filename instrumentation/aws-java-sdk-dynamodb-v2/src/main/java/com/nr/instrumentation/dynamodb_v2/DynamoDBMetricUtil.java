package com.nr.instrumentation.dynamodb_v2;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.TracedMethod;

import java.net.URI;

public abstract class DynamoDBMetricUtil {

    private static final String PRODUCT = DatastoreVendor.DynamoDB.name();
    private static final String INSTANCE_HOST = "amazon";
    private static final String INSTANCE_ID = "dynamodb";

    public static void metrics(TracedMethod tracedMethod, String operation, String collection, URI endpoint) {
        String host = endpoint == null ? INSTANCE_HOST : endpoint.getHost();
        String port = endpoint == null ? INSTANCE_ID : String.valueOf(getPort(endpoint));

        DatastoreParameters params = DatastoreParameters
                .product(PRODUCT)
                .collection(collection)
                .operation(operation)
                .instance(host, port)
                .noDatabaseName()
                .build();

        tracedMethod.reportAsExternal(params);
    }

    private static int getPort(URI endpoint) {
        if (endpoint.getPort() > 0) {
            return endpoint.getPort();
        }

        final String scheme = endpoint.getScheme();
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        } else if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        return -1;
    }

}

