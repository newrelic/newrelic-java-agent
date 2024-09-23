/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.dynamodb_1_11_106;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;

import java.net.URI;
import java.util.logging.Level;

/**
 * This uses {@link DatastoreParameters} to create external metrics for all DynamoDB calls in
 * {@link com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient} and {@link com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient}.
 */
public abstract class DynamoDBMetricUtil {

    private static final String PRODUCT = DatastoreVendor.DynamoDB.name();
    private static final String INSTANCE_HOST = "amazon";


    public static void metrics(TracedMethod tracedMethod, String operation, String collection, URI endpoint, Object sdkClient) {
        String host = INSTANCE_HOST;
        String arn = null;
        Integer port = null;
        if (endpoint != null) {
            host = endpoint.getHost();
            port = getPort(endpoint);
            arn = getArn(collection, sdkClient, host);
        }
        DatastoreParameters params = DatastoreParameters
                .product(PRODUCT)
                .collection(collection)
                .operation(operation)
                .instance(host, port)
                .noDatabaseName()
                .cloudResourceId(arn)
                .build();

        tracedMethod.reportAsExternal(params);
    }

    // visible for testing
    static String getArn(String tableName, Object sdkClient, String host) {
        String accountId = AgentBridge.cloud.getAccountInfo(sdkClient, CloudAccountInfo.AWS_ACCOUNT_ID);
        if (accountId == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. No account information provided.");
            return null;
        }

        if (tableName == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Unable to determine table.");
            return null;
        }

        String region = getRegion(host);
        if (region == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Unable to determine region.");
            return null;
        }

        // arn:${Partition}:dynamodb:${Region}:${Account}:table/${TableName}
        return "arn:aws:dynamodb:" + region + ":" + accountId + ":table/" + tableName;
    }

    // visible for testing
    static String getRegion(String host) {
        if (host == null) {
            return null;
        }
        if (!host.startsWith("dynamodb")) {
            return null;
        }

        final int afterDynamoDb = 8; // "dynamodb".length()
        if (host.charAt(afterDynamoDb) == '.') {
            // dynamodb.{region}.amazonaws.com
            int secondPeriod = host.indexOf('.', afterDynamoDb + 1);
            if (secondPeriod > 0 && host.startsWith(".amazonaws.com", secondPeriod)) {
                return host.substring(afterDynamoDb + 1, secondPeriod);
            } else {
                return null;
            }
        } else if (host.startsWith("-fips.", afterDynamoDb)) {
            // dynamodb-fips.{region}.amazonaws.com
            final int firstPeriod = 13; // "dynamodb-fips".length()
            int secondPeriod = host.indexOf('.', firstPeriod + 1);
            if (secondPeriod > 0 && host.startsWith(".amazonaws.com", secondPeriod)) {
                return host.substring(firstPeriod + 1, secondPeriod);
            } else {
                return null;
            }
        }
        return null;
    }

    private static Integer getPort(URI endpoint) {
        if (endpoint.getPort() > 0) {
            return endpoint.getPort();
        }

        final String scheme = endpoint.getScheme();
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        } else if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        return null;
    }

}
