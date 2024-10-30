/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.dynamodb_1_11_106;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.CloudApi;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.TracedMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DynamoDBMetricUtilTest {

    private CloudApi previousCloudApi;

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
    public void testGetArn() {
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        String table = "test";
        String host = "dynamodb.us-east-2.amazonaws.com";

        String arn = DynamoDBMetricUtil.getArn(table, sdkClient, host);
        assertEquals("arn:aws:dynamodb:us-east-2:123456789:table/test", arn);
    }

    @Test
    public void testGetArn_withoutAccountId() {
        Object sdkClient = new Object();
        String table = "test";
        String host = "dynamodb.us-east-2.amazonaws.com";

        String arn = DynamoDBMetricUtil.getArn(table, sdkClient, host);
        assertNull(arn);
    }

    @Test
    public void testGetArn_withoutTable() {
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        String host = "dynamodb.us-east-2.amazonaws.com";

        String arn = DynamoDBMetricUtil.getArn(null, sdkClient, host);
        assertNull(arn);
    }

    @Test
    public void testGetArn_withoutHost() {
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        String table = "test";

        String arn = DynamoDBMetricUtil.getArn(table, sdkClient, null);
        assertNull(arn);
    }

    @Test
    public void testMetrics() {
        Object sdkClient = new Object();
        when(AgentBridge.cloud.getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789");
        String table = "test";
        TracedMethod tracedMethod = mock(TracedMethod.class);
        String operation = "getItem";
        URI endpoint = URI.create("https://dynamodb.us-east-2.amazonaws.com");

        DynamoDBMetricUtil.metrics(tracedMethod, operation, table, endpoint, sdkClient);

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

}