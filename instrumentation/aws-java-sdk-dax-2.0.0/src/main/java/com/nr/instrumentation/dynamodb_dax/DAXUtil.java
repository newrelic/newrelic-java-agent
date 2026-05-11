/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.dynamodb_dax;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.dax.Configuration;

import java.util.logging.Level;

public abstract class DAXUtil {

    private static final String PRODUCT = DatastoreVendor.DAX.name();
    private static final String INSTANCE_HOST = "amazon";
    private static final int DEFAULT_DAX_PORT = 8111;

    public static void recordExternal(TracedMethod tracedMethod, String operation, String tableName,
            Object sdkClient, Configuration configuration) {

        String host = INSTANCE_HOST;
        Integer port = null;
        String arn = null;
        if (configuration != null) {
            String configHost = configuration.host();
            if (configHost != null && !configHost.isEmpty()) {
                host = configHost;
            }
            port = configuration.port();
            if (port <= 0) {
                port = DEFAULT_DAX_PORT;
            }
            arn = getArn(tableName, sdkClient, configuration);
        }

        NewRelic.getAgent().getLogger().log(Level.FINEST, "AWSDAX: Recording {0} external for collection {1}",
                operation, tableName);

        DatastoreParameters params = DatastoreParameters
                .product(PRODUCT)
                .collection(tableName)
                .operation(operation)
                .instance(host, port)
                .noDatabaseName()
                .cloudResourceId(arn)
                .build();

        tracedMethod.reportAsExternal(params);
    }

    // visible for testing
    static String getArn(String tableName, Object sdkClient, Configuration configuration) {
        if (tableName == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "AWSDAX: Unable to assemble ARN. Table name is null.");
            return null;
        }

        String region = findRegion(configuration);
        if (region == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "AWSDAX: Unable to assemble ARN. Region is null.");
            return null;
        }
        String accountId = getAccountId(sdkClient, configuration);
        if (accountId == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "AWSDAX: Unable to assemble ARN. Unable to retrieve account information.");
            return null;
        }
        // arn:${Partition}:dynamodb:${Region}:${Account}:table/${TableName}
        NewRelic.getAgent().getLogger().log(Level.FINEST, "AWSDAX: Returning arn");
        return "arn:aws:dynamodb:" + region + ":" + accountId + ":table/" + tableName;
    }

    private static String getAccountId(Object sdkClient, Configuration configuration) {
        String accountId = AgentBridge.cloud.getAccountInfo(sdkClient, CloudAccountInfo.AWS_ACCOUNT_ID);
        if (accountId != null) {
            return accountId;
        }

        AwsCredentialsProvider credentialsProvider = configuration.credentialsProvider();
        if (credentialsProvider != null) {
            AwsCredentials credentials = credentialsProvider.resolveCredentials();
            if (credentials != null) {
                String accessKey = credentials.accessKeyId();
                if (accessKey != null) {
                    return AgentBridge.cloud.decodeAwsAccountId(accessKey);
                }
            }
        }
        return null;
    }

    // visible for testing
    static String findRegion(Configuration configuration) {
        Region awsRegion = configuration.region();
        return awsRegion == null ? null : awsRegion.id();
    }
}
