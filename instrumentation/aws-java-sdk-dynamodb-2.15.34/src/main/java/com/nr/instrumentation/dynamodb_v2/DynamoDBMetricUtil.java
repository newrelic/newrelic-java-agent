package com.nr.instrumentation.dynamodb_v2;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.agent.util.AwsAccountUtil;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.util.logging.Level;

public abstract class DynamoDBMetricUtil {

    private static final String PRODUCT = DatastoreVendor.DynamoDB.name();
    private static final String INSTANCE_HOST = "amazon";
    private static final String INSTANCE_ID = "dynamodb";

    public static void metrics(TracedMethod tracedMethod, String operation, String tableName, SdkClientConfiguration clientConfiguration) {

        String host = INSTANCE_HOST;
        String port = INSTANCE_ID;
        String arn = null;
        if (clientConfiguration != null) {
            URI endpoint = clientConfiguration.option(SdkClientOption.ENDPOINT);
            if (endpoint != null) {
                host = endpoint.getHost();
                port = String.valueOf(getPort(endpoint));
            }
            arn = getArn(tableName, clientConfiguration);
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

    private static String getArn(String tableName, SdkClientConfiguration clientConfiguration) {
        if (tableName == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Table name is null.");
            return null;
        }
        AwsCredentialsProvider credentialsProvider = clientConfiguration.option(AwsClientOption.CREDENTIALS_PROVIDER);
        if (credentialsProvider == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Credentials provider is null.");
            return null;
        }
        Region region = clientConfiguration.option(AwsClientOption.AWS_REGION);
        if (region == null || region.id() == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Region is null.");
            return null;
        }
        String accessKey = credentialsProvider.resolveCredentials().accessKeyId();
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
        return "arn:aws:dynamodb:" + region.id() + ":" + accountId + ":table/" + tableName;
    }

    private static int getPort(URI endpoint) {
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
        return -1;
    }
}

