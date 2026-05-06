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
import software.amazon.awssdk.regions.Region;
import software.amazon.dax.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DAXUtilTest {

    private CloudApi previousCloudApi;
    private Configuration configuration;

    @Before
    public void setup() {
        previousCloudApi = AgentBridge.cloud;
        AgentBridge.cloud = mock(CloudApi.class);
    }

    @After
    public void tearDown() {
        AgentBridge.cloud = previousCloudApi;
    }

    @Test
    public void testFindRegion() {
        configuration = createConfigurationWithRegion(Region.US_EAST_2);
        assertEquals("us-east-2", DAXUtil.findRegion(configuration));
    }

    @Test
    public void testFindRegion_default() {
        // null region defaults to us-east-1
        configuration = createConfigurationWithRegion(null);
        assertEquals("us-east-1", DAXUtil.findRegion(configuration));
    }


    @Test
    public void testGetArn() {
        configuration = createConfigurationWithRegion(Region.US_EAST_2);
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        String table = "test";

        String arn = DAXUtil.getArn(table, sdkClient, configuration);
        assertEquals("arn:aws:dynamodb:us-east-2:123456789:table/test", arn);
    }

    @Test
    public void testGetArn_withAccessKey() {
        Object sdkClient = new Object();
        String table = "test";
        configuration = createConfigurationWithRegion(Region.US_EAST_2);

        when(AgentBridge.cloud.decodeAwsAccountId(anyString())).thenReturn("123456789");

        String arn = DAXUtil.getArn(table, sdkClient, configuration);
        assertEquals("arn:aws:dynamodb:us-east-2:123456789:table/test", arn);
    }

    @Test
    public void testGetArn_withoutAccountId() {
        Object sdkClient = new Object();
        String table = "test";
        configuration = createConfigurationWithRegion(Region.US_EAST_2);

        String arn = DAXUtil.getArn(table, sdkClient, configuration);
        assertNull(arn);
    }

    @Test
    public void testGetArn_withoutTable() {
        Object sdkClient = new Object();
        configuration = createConfigurationWithRegion(Region.US_EAST_2);
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");

        String arn = DAXUtil.getArn(null, sdkClient, configuration);
        assertNull(arn);
    }

    @Test
    public void testGetArn_withoutRegion() {
        // null region defaults to us-east-1
        Object sdkClient = new Object();
        configuration = createConfigurationWithRegion(null);
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        String table = "test";

        String arn = DAXUtil.getArn(table, sdkClient, configuration);
        assertEquals("arn:aws:dynamodb:us-east-1:123456789:table/test", arn);
    }

    @Test
    public void testRecordExternal() {
        Object sdkClient = new Object();
        configuration = createConfigurationWithRegion(Region.US_EAST_2);
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        String table = "test";
        TracedMethod tracedMethod = mock(TracedMethod.class);
        String operation = "getItem";

        DAXUtil.recordExternal(tracedMethod, operation, table, sdkClient, configuration);

        ArgumentCaptor<DatastoreParameters> externalParamsCaptor = ArgumentCaptor.forClass(DatastoreParameters.class);
        verify(tracedMethod).reportAsExternal(externalParamsCaptor.capture());
        DatastoreParameters params = externalParamsCaptor.getValue();
        assertEquals("DAX", params.getProduct());
        assertEquals("test", params.getCollection());
        assertEquals("getItem", params.getOperation());
        assertEquals("my-cluster.l6fzcv.dax-clusters.us-east-2.amazonaws.com", params.getHost());
        assertEquals(Integer.valueOf(8111), params.getPort());
        assertNull(params.getPathOrId());
        assertEquals("arn:aws:dynamodb:us-east-2:123456789:table/test", params.getCloudResourceId());
        assertNull(params.getDatabaseName());
    }

    @Test
    public void testRecordExternal_withoutClientConfig() {
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        String table = "test";
        TracedMethod tracedMethod = mock(TracedMethod.class);
        String operation = "getItem";

        DAXUtil.recordExternal(tracedMethod, operation, table, sdkClient, null);

        ArgumentCaptor<DatastoreParameters> externalParamsCaptor = ArgumentCaptor.forClass(DatastoreParameters.class);
        verify(tracedMethod).reportAsExternal(externalParamsCaptor.capture());
        DatastoreParameters params = externalParamsCaptor.getValue();
        assertEquals("DAX", params.getProduct());
        assertEquals("test", params.getCollection());
        assertEquals("getItem", params.getOperation());
        assertEquals("amazon", params.getHost());
        assertNull(params.getPort());
        assertNull(params.getPathOrId());
        assertNull(params.getCloudResourceId());
        assertNull(params.getDatabaseName());
    }

    @Test
    public void testRecordExternal_emptyHost() {
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        // Configuration with empty URL results in empty host
        Configuration config = createConfigurationWithEmptyHost();
        TracedMethod tracedMethod = mock(TracedMethod.class);

        DAXUtil.recordExternal(tracedMethod, "getItem", "test", sdkClient, config);

        ArgumentCaptor<DatastoreParameters> captor = ArgumentCaptor.forClass(DatastoreParameters.class);
        verify(tracedMethod).reportAsExternal(captor.capture());
        // Should fall back to default host when host is empty
        assertEquals("amazon", captor.getValue().getHost());
    }

    @Test
    public void testRecordExternal_nullCredentialsProvider() {
        Object sdkClient = new Object();
        // No cloud account info and no credentials provider
        Configuration config = createConfigurationWithNullCredentials();
        TracedMethod tracedMethod = mock(TracedMethod.class);

        DAXUtil.recordExternal(tracedMethod, "getItem", "test", sdkClient, config);

        ArgumentCaptor<DatastoreParameters> captor = ArgumentCaptor.forClass(DatastoreParameters.class);
        verify(tracedMethod).reportAsExternal(captor.capture());
        // ARN should be null when account ID cannot be determined
        assertNull(captor.getValue().getCloudResourceId());
    }

    @Test(expected = software.amazon.awssdk.core.exception.SdkClientException.class)
    public void testConfigurationBuild_credentialsResolveReturnsNull_throwsException() {
        // When credentials cannot be resolved, the AWS SDK throws during Configuration.build()
        AwsCredentialsProvider credentialsProvider = mock(AwsCredentialsProvider.class);
        when(credentialsProvider.resolveCredentials()).thenReturn(null);

        Configuration.builder()
                .region(Region.US_EAST_2)
                .credentialsProvider(credentialsProvider)
                .url("dax://my-cluster.l6fzcv.dax-clusters.us-east-2.amazonaws.com")
                .build();
    }

    @Test
    public void testGetArn_batchOperation() {
        Object sdkClient = new Object();
        configuration = createConfigurationWithRegion(Region.US_EAST_2);
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");

        String arn = DAXUtil.getArn("batch", sdkClient, configuration);
        assertEquals("arn:aws:dynamodb:us-east-2:123456789:table/batch", arn);
    }

    @Test
    public void testGetArn_transactionOperation() {
        Object sdkClient = new Object();
        configuration = createConfigurationWithRegion(Region.US_EAST_2);
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");

        String arn = DAXUtil.getArn("transaction", sdkClient, configuration);
        assertEquals("arn:aws:dynamodb:us-east-2:123456789:table/transaction", arn);
    }

    @Test
    public void testRecordExternal_batchGetItem() {
        Object sdkClient = new Object();
        configuration = createConfigurationWithRegion(Region.US_EAST_2);
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        TracedMethod tracedMethod = mock(TracedMethod.class);

        DAXUtil.recordExternal(tracedMethod, "batchGetItem", "batch", sdkClient, configuration);

        ArgumentCaptor<DatastoreParameters> captor = ArgumentCaptor.forClass(DatastoreParameters.class);
        verify(tracedMethod).reportAsExternal(captor.capture());
        DatastoreParameters params = captor.getValue();
        assertEquals("batchGetItem", params.getOperation());
        assertEquals("batch", params.getCollection());
        assertEquals("arn:aws:dynamodb:us-east-2:123456789:table/batch", params.getCloudResourceId());
    }

    @Test
    public void testRecordExternal_transactWriteItems() {
        Object sdkClient = new Object();
        configuration = createConfigurationWithRegion(Region.US_EAST_2);
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        TracedMethod tracedMethod = mock(TracedMethod.class);

        DAXUtil.recordExternal(tracedMethod, "transactWriteItems", "transaction", sdkClient, configuration);

        ArgumentCaptor<DatastoreParameters> captor = ArgumentCaptor.forClass(DatastoreParameters.class);
        verify(tracedMethod).reportAsExternal(captor.capture());
        DatastoreParameters params = captor.getValue();
        assertEquals("transactWriteItems", params.getOperation());
        assertEquals("transaction", params.getCollection());
        assertEquals("arn:aws:dynamodb:us-east-2:123456789:table/transaction", params.getCloudResourceId());
    }

    private Configuration createConfigurationWithRegion(Region region) {
        AwsCredentialsProvider credentialsProvider;
        credentialsProvider = mock(AwsCredentialsProvider.class, RETURNS_DEEP_STUBS);
        when(credentialsProvider.resolveCredentials().accessKeyId()).thenReturn("accessKey");

        return Configuration.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .url("dax://my-cluster.l6fzcv.dax-clusters.us-east-2.amazonaws.com")
                .build();
    }

    private Configuration createConfigurationWithEmptyHost() {
        AwsCredentialsProvider credentialsProvider = mock(AwsCredentialsProvider.class, RETURNS_DEEP_STUBS);
        when(credentialsProvider.resolveCredentials().accessKeyId()).thenReturn("accessKey");

        // Using endpoints() with empty list or minimal config to get empty host
        return Configuration.builder()
                .region(Region.US_EAST_2)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    private Configuration createConfigurationWithNullCredentials() {
        return Configuration.builder()
                .region(Region.US_EAST_2)
                .url("dax://my-cluster.l6fzcv.dax-clusters.us-east-2.amazonaws.com")
                .build();
    }
}
