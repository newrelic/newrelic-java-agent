/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.dynamodb_1_11_106;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.util.AwsHostNameUtils;
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


    public static void metrics(TracedMethod tracedMethod, String operation, String collection, URI endpoint, Object sdkClient,
            AWSCredentialsProvider credentialsProvider) {
        String host = INSTANCE_HOST;
        String arn = null;
        Integer port = null;
        if (endpoint != null) {
            host = endpoint.getHost();
            port = getPort(endpoint);
            arn = getArn(collection, sdkClient, host, credentialsProvider);
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
    static String getArn(String tableName, Object sdkClient, String host, AWSCredentialsProvider credentialsProvider) {
        if (host == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Host is null.");
            return null;
        }

        if (tableName == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Unable to determine table.");
            return null;
        }

        String region = AwsHostNameUtils.parseRegion(host, "dynamodb");
        if (region == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Unable to determine region.");
            return null;
        }
        String accountId = getAccountId(sdkClient, credentialsProvider);
        if (accountId == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Unable to retrieve account information.");
            return null;
        }
        // arn:${Partition}:dynamodb:${Region}:${Account}:table/${TableName}
        return "arn:aws:dynamodb:" + region + ":" + accountId + ":table/" + tableName;
    }

    private static String getAccountId(Object sdkClient, AWSCredentialsProvider credentialsProvider) {
        String accountId = AgentBridge.cloud.getAccountInfo(sdkClient, CloudAccountInfo.AWS_ACCOUNT_ID);
        if (accountId != null) {
            return accountId;
        }

        AWSCredentials credentials = credentialsProvider.getCredentials();
        if (credentials != null) {
            String accessKey = credentials.getAWSAccessKeyId();
            if (accessKey != null) {
                return AgentBridge.cloud.decodeAwsAccountId(accessKey);
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
