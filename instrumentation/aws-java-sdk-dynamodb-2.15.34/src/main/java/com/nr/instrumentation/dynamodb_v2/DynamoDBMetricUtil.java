package com.nr.instrumentation.dynamodb_v2;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.util.AwsHostNameUtils;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.util.logging.Level;

public abstract class DynamoDBMetricUtil {

    private static final String PRODUCT = DatastoreVendor.DynamoDB.name();
    private static final String INSTANCE_HOST = "amazon";

    public static void metrics(TracedMethod tracedMethod, String operation, String tableName, Object sdkClient, SdkClientConfiguration clientConfiguration) {

        String host = INSTANCE_HOST;
        Integer port = null;
        String arn = null;
        if (clientConfiguration != null) {
            URI endpoint = clientConfiguration.option(SdkClientOption.ENDPOINT);
            if (endpoint != null) {
                host = endpoint.getHost();
                port = getPort(endpoint);
            }
            arn = getArn(tableName, sdkClient, clientConfiguration, host);
        }
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
    static String getArn(String tableName, Object sdkClient, SdkClientConfiguration clientConfiguration, String host) {
        String accountId = AgentBridge.cloud.getAccountInfo(sdkClient, CloudAccountInfo.AWS_ACCOUNT_ID);
        if (accountId == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. No account information provided.");
            return null;
        }

        if (tableName == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Table name is null.");
            return null;
        }

        String region = findRegion(clientConfiguration, host);
        if (region == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Region is null.");
            return null;
        }

        // arn:${Partition}:dynamodb:${Region}:${Account}:table/${TableName}
        return "arn:aws:dynamodb:" + region + ":" + accountId + ":table/" + tableName;
    }

    // visible for testing
    static String findRegion(SdkClientConfiguration clientConfig, String host) {
        Boolean endpointOverridden = clientConfig.option(SdkClientOption.ENDPOINT_OVERRIDDEN);
        if (endpointOverridden == Boolean.TRUE) { // endpointOverridden could be null
            Region dynamodb = AwsHostNameUtils.parseSigningRegion(host, "dynamodb").orElse(null);
            return dynamodb == null ? null : dynamodb.id();
        }

        Region awsRegion = clientConfig.option(AwsClientOption.AWS_REGION);
        if (awsRegion != null) {
            return awsRegion.id();
        }

        return null;
    }

    private static Integer getPort(URI endpoint) {
        int port = endpoint.getPort();
        if (port > 0) {
            return port;
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

