/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk2.services.lambda;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.CloudApi;
import com.newrelic.agent.bridge.NoOpCloud;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.CloudParameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.regions.Region;

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
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("my-function", null));
        assertEquals("", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithAlias() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("my-function:alias", null));
        assertEquals("", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithAliasClient() {
        mockCloudApiClient();
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("my-function:alias", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithVersion() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("my-function:123", null));
        assertEquals("", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithVersionClient() {
        mockCloudApiClient();
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("my-function:123", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndAliasQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("my-function", "alias"));
        assertEquals("", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndAliasQualifierClient() {
        mockCloudApiClient();
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("my-function", "alias"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndVersionQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("my-function", "123"));
        assertEquals("", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndVersionQualifierClient() {
        mockCloudApiClient();
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("my-function", "123"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnPartialArn() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("123456789012:function:my-function", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnWithAlias() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("123456789012:function:my-function:alias", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnWithVersion() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("123456789012:function:my-function:123", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnAndAliasQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("123456789012:function:my-function", "alias"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnAndVersionQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("123456789012:function:my-function", "123"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFullArn() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("arn:aws:lambda:us-east-1:123456789012:function:my-function", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFullArnWithAlias() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(
                data("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFullArnWithVersion() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", null));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFullArnAndAliasQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("arn:aws:lambda:us-east-1:123456789012:function:my-function", "alias"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFullArnAndVersionQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("arn:aws:lambda:us-east-1:123456789012:function:my-function", "123"));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnDifferentRegion() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data("arn:aws:lambda:us-west-2:123456789012:function:my-function", null));
        assertEquals("arn:aws:lambda:us-west-2:123456789012:function:my-function", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    private FunctionRawData data(String functionRef, String number) {
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                .option(AwsClientOption.AWS_REGION, Region.US_EAST_1)
                .build();
        return new FunctionRawData(functionRef, number, config, new Object());
    }

    private static void mockCloudApiClient() {
        when(AgentBridge.cloud.getAccountInfo(any(), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                .thenReturn("123456789012");
    }
}