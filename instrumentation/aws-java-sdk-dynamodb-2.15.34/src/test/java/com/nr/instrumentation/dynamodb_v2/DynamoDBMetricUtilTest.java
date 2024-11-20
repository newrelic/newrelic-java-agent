/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.dynamodb_v2;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.CloudApi;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.TracedMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DynamoDBMetricUtilTest {

    private CloudApi previousCloudApi;

    private AwsCredentialsProvider credentialsProvider;

    @Before
    public void setup() {
        previousCloudApi = AgentBridge.cloud;
        AgentBridge.cloud = mock(CloudApi.class);
        credentialsProvider = mock(AwsCredentialsProvider.class, RETURNS_DEEP_STUBS);
        when(credentialsProvider.resolveCredentials().accessKeyId()).thenReturn("accessKey");
    }

    @After
    public void tearDown() {
        AgentBridge.cloud = previousCloudApi;
    }

    @Test
    public void testFindRegion() {
        SdkClientConfiguration clientConfig = SdkClientConfiguration.builder()
                .option(AwsClientOption.CREDENTIALS_PROVIDER, credentialsProvider)
                .option(AwsClientOption.AWS_REGION, Region.US_WEST_2)
                .build();
        assertEquals("us-west-2", DynamoDBMetricUtil.findRegion(clientConfig));
    }

    @Test
    public void testFindRegion_fail() {
        SdkClientConfiguration clientConfig = SdkClientConfiguration.builder()
                .option(AwsClientOption.CREDENTIALS_PROVIDER, credentialsProvider)
                .build();
        assertNull(DynamoDBMetricUtil.findRegion(clientConfig));
    }


    @Test
    public void testGetArn() {
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                .option(AwsClientOption.CREDENTIALS_PROVIDER, credentialsProvider)
                .option(AwsClientOption.AWS_REGION, Region.US_EAST_2)
                .build();
        String table = "test";

        String arn = DynamoDBMetricUtil.getArn(table, sdkClient, config);
        assertEquals("arn:aws:dynamodb:us-east-2:123456789:table/test", arn);
    }

    @Test
    public void testGetArn_witAccessKey() {
        Object sdkClient = new Object();
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                .option(AwsClientOption.CREDENTIALS_PROVIDER, credentialsProvider)
                .option(AwsClientOption.AWS_REGION, Region.US_EAST_2)
                .option(SdkClientOption.ENDPOINT, URI.create("https://dynamodb.us-east-2.amazonaws.com"))
                .build();
        String table = "test";
        when(AgentBridge.cloud.decodeAwsAccountId(anyString())).thenReturn("123456789");


        String arn = DynamoDBMetricUtil.getArn(table, sdkClient, config);
        assertEquals("arn:aws:dynamodb:us-east-2:123456789:table/test", arn);
    }

    @Test
    public void testGetArn_withoutAccountId() {
        Object sdkClient = new Object();
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                .option(AwsClientOption.CREDENTIALS_PROVIDER, credentialsProvider)
                .option(AwsClientOption.AWS_REGION, Region.US_EAST_2)
                .option(SdkClientOption.ENDPOINT, URI.create("https://dynamodb.us-east-2.amazonaws.com"))
                .build();
        String table = "test";

        String arn = DynamoDBMetricUtil.getArn(table, sdkClient, config);
        assertNull(arn);
    }

    @Test
    public void testGetArn_withoutTable() {
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                .option(AwsClientOption.CREDENTIALS_PROVIDER, credentialsProvider)
                .option(AwsClientOption.AWS_REGION, Region.US_EAST_2)
                .build();

        String arn = DynamoDBMetricUtil.getArn(null, sdkClient, config);
        assertNull(arn);
    }

    @Test
    public void testGetArn_withoutRegion() {
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                .option(AwsClientOption.CREDENTIALS_PROVIDER, credentialsProvider)
                .build();
        String table = "test";

        String arn = DynamoDBMetricUtil.getArn(table, sdkClient, config);
        assertNull(arn);
    }

    @Test
    public void testMetrics() {
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                .option(AwsClientOption.CREDENTIALS_PROVIDER, credentialsProvider)
                .option(AwsClientOption.AWS_REGION, Region.US_EAST_2)
                .option(SdkClientOption.ENDPOINT, URI.create("https://dynamodb.us-east-2.amazonaws.com"))
                .build();
        String table = "test";
        TracedMethod tracedMethod = mock(TracedMethod.class);
        String operation = "getItem";

        DynamoDBMetricUtil.metrics(tracedMethod, operation, table, sdkClient, config);

        ArgumentCaptor<DatastoreParameters> externalParamsCaptor = ArgumentCaptor.forClass(DatastoreParameters.class);
        verify(tracedMethod).reportAsExternal(externalParamsCaptor.capture());
        DatastoreParameters params = externalParamsCaptor.getValue();
        assertEquals("DynamoDB", params.getProduct());
        assertEquals("test", params.getCollection());
        assertEquals("getItem", params.getOperation());
        assertEquals("dynamodb.us-east-2.amazonaws.com", params.getHost());
        assertEquals(Integer.valueOf(443), params.getPort());
        assertNull(params.getPathOrId());
        assertEquals("arn:aws:dynamodb:us-east-2:123456789:table/test", params.getCloudResourceId());
        assertNull(params.getDatabaseName());
    }

    @Test
    public void testMetrics_withoutClientConfig() {
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        String table = "test";
        TracedMethod tracedMethod = mock(TracedMethod.class);
        String operation = "getItem";

        DynamoDBMetricUtil.metrics(tracedMethod, operation, table, sdkClient, null);

        ArgumentCaptor<DatastoreParameters> externalParamsCaptor = ArgumentCaptor.forClass(DatastoreParameters.class);
        verify(tracedMethod).reportAsExternal(externalParamsCaptor.capture());
        DatastoreParameters params = externalParamsCaptor.getValue();
        assertEquals("DynamoDB", params.getProduct());
        assertEquals("test", params.getCollection());
        assertEquals("getItem", params.getOperation());
        assertEquals("amazon", params.getHost());
        assertNull(params.getPort());
        assertNull(params.getPathOrId());
        assertNull(params.getCloudResourceId());
        assertNull(params.getDatabaseName());
    }
}