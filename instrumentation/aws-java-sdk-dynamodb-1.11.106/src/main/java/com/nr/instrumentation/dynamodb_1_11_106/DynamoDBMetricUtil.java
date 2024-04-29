/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.dynamodb_1_11_106;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.util.AwsAccountUtil;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;

import java.net.URI;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This uses {@link DatastoreParameters} to create external metrics for all DynamoDB calls in
 * {@link com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient} and {@link com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient}.
 */
public abstract class DynamoDBMetricUtil {

    private static final String PRODUCT = DatastoreVendor.DynamoDB.name();
    private static final String INSTANCE_HOST = "amazon";
    private static final String INSTANCE_ID = "dynamodb";
    private static final Pattern REGION_PATTERN = Pattern.compile("dynamodb\\.([^.]*)\\.amazonaws\\.com");


    public static void metrics(TracedMethod tracedMethod, String operation, String collection, URI endpoint, AWSCredentialsProvider credentialsProvider) {
        String host = INSTANCE_HOST;
        String region = null;
        if (endpoint != null) {
            host = endpoint.getHost();
            region = getRegion(host);
        }
        String port = endpoint == null ? INSTANCE_ID : String.valueOf(getPort(endpoint));
        String arn = getArn(collection, region, credentialsProvider);
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

    private static String getArn(String tableName, String region, AWSCredentialsProvider credentialsProvider) {
        if (credentialsProvider == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Credentials provider is null.");
            return null;
        }
        if (tableName == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Table name is null.");
            return null;
        }
        if (region == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Region is null.");
            return null;
        }
        AWSCredentials credentials = credentialsProvider.getCredentials();
        if (credentials == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Credentials is null.");
            return null;
        }
        String accessKey = credentials.getAWSAccessKeyId();
        if (accessKey == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Access key is null.");
            return null;
        }
        Long accountId = AwsAccountUtil.get().decodeAccount(accessKey);
        if (accountId == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Unable to decode account.");
            return null;
        }
        // arn:${Partition}:dynamodb:${Region}:${Account}:table/${TableName}
        String arn = "arn:aws:dynamodb:" + region + ":" + accountId + ":table/" + tableName;
        return arn;
    }

    /**
     * @throws NullPointerException if host is null
     */
    private static String getRegion(String host) {
        Matcher matcher = REGION_PATTERN.matcher(host);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
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
