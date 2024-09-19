/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk1.services.lambda;

import com.amazonaws.regions.Regions;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.CloudApi;
import com.newrelic.agent.bridge.NoOpCloud;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.CloudParameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LambdaUtilTest {

    @Before
    public void before() {
        AgentBridge.cloud = mock(CloudApi.class);
    }

    @After
    public void after() {
        AgentBridge.cloud = NoOpCloud.INSTANCE;
    }

    @Test
    public void testGetCloudParamFunctionName() {
        FunctionRawData functionRawData = data("my-function", null);
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        assertNotNull(cloudParameters);
        assertEquals("aws_lambda", cloudParameters.getPlatform());
        assertNull(cloudParameters.getResourceId());
    }

    @Test
    public void testGetCloudParamPartialArn() {
        FunctionRawData functionRawData = data("123456789012:function:my-function", null);
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        assertNotNull(cloudParameters);
        assertEquals("aws_lambda", cloudParameters.getPlatform());
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", cloudParameters.getResourceId());
    }

    @Test
    public void testGetCloudParamArnQualifier() {
        FunctionRawData functionRawData = data("arn:aws:lambda:us-east-1:123456789012:function:my-function", "alias");
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        assertNotNull(cloudParameters);
        assertEquals("aws_lambda", cloudParameters.getPlatform());
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", cloudParameters.getResourceId());
    }

    @Test
    public void testGetArnFunctionName() {
        FunctionProcessedData data = LambdaUtil.processData(data("my-function", null));
        assertEquals("", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameClient() {
        mockCloudApiClient();
        FunctionProcessedData data = LambdaUtil.processData(data("my-function", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithAlias() {
        FunctionProcessedData data = LambdaUtil.processData(data("my-function:alias", null));
        assertEquals("", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithAliasClient() {
        mockCloudApiClient();
        FunctionProcessedData data = LambdaUtil.processData(data("my-function:alias", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithVersion() {
        FunctionProcessedData data = LambdaUtil.processData(data("my-function:123", null));
        assertEquals("", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithVersionClient() {
        mockCloudApiClient();
        FunctionProcessedData data = LambdaUtil.processData(data("my-function:123", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndAliasQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(data("my-function", "alias"));
        assertEquals("", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndAliasQualifierClient() {
        mockCloudApiClient();
        FunctionProcessedData data = LambdaUtil.processData(data("my-function", "alias"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndVersionQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(data("my-function", "123"));
        assertEquals("", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndVersionQualifierClient() {
        mockCloudApiClient();
        FunctionProcessedData data = LambdaUtil.processData(data("my-function", "123"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnPartialArn() {
        FunctionProcessedData data = LambdaUtil.processData(data("123456789012:function:my-function", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnWithAlias() {
        FunctionProcessedData data = LambdaUtil.processData(data("123456789012:function:my-function:alias", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnWithVersion() {
        FunctionProcessedData data = LambdaUtil.processData(data("123456789012:function:my-function:123", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnAndAliasQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(data("123456789012:function:my-function", "alias"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnAndVersionQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(data("123456789012:function:my-function", "123"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFullArn() {
        FunctionProcessedData data = LambdaUtil.processData(data("arn:aws:lambda:us-east-1:123456789012:function:my-function", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFullArnWithAlias() {
        FunctionProcessedData data = LambdaUtil.processData(data("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFullArnWithVersion() {
        FunctionProcessedData data = LambdaUtil.processData(data("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFullArnAndAliasQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(data("arn:aws:lambda:us-east-1:123456789012:function:my-function", "alias"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFullArnAndVersionQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(data("arn:aws:lambda:us-east-1:123456789012:function:my-function", "123"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnDifferentRegion() {
        FunctionProcessedData data = LambdaUtil.processData(data("arn:aws:lambda:us-west-2:123456789012:function:my-function", null));
        assertEquals("arn:aws:lambda:us-west-2:123456789012:function:my-function", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    private static void mockCloudApiClient() {
        when(AgentBridge.cloud.getAccountInfo(any(), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789012");
    }

    private FunctionRawData data(String functionRef, String qualifier) {
        return new FunctionRawData(functionRef, qualifier, Regions.US_EAST_1.getName(), this);
    }
}