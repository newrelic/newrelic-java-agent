/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk2.services.lambda;

import com.newrelic.api.agent.CloudParameters;
import org.junit.Test;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class LambdaUtilTest {

    @Test
    public void testGetCloudParamFunctionName() {
        FunctionRawData functionRawData = new FunctionRawData("my-function", null, getConfig());
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        assertNotNull(cloudParameters);
        assertEquals("aws_lambda", cloudParameters.getPlatform());
        assertNull(cloudParameters.getResourceId());
    }

    @Test
    public void testGetCloudParamPartialArn() {
        FunctionRawData functionRawData = new FunctionRawData("123456789012:function:my-function", null, getConfig());
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        assertNotNull(cloudParameters);
        assertEquals("aws_lambda", cloudParameters.getPlatform());
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", cloudParameters.getResourceId());
    }

    @Test
    public void testGetCloudParamArnQualifier() {
        FunctionRawData functionRawData = new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function", "alias", getConfig());
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        assertNotNull(cloudParameters);
        assertEquals("aws_lambda", cloudParameters.getPlatform());
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", cloudParameters.getResourceId());
    }

    @Test
    public void testGetArnFunctionName() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("my-function", null, getConfig()));
        assertEquals("", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithAlias() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("my-function:alias", null, getConfig()));
        assertEquals("", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithVersion() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("my-function:123", null, getConfig()));
        assertEquals("", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndAliasQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("my-function", "alias", getConfig()));
        assertEquals("", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndVersionQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("my-function", "123", getConfig()));
        assertEquals("", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnPartialArn() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("123456789012:function:my-function", null, getConfig()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnWithAlias() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("123456789012:function:my-function:alias", null, getConfig()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnWithVersion() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("123456789012:function:my-function:123", null, getConfig()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnAndAliasQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("123456789012:function:my-function", "alias", getConfig()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnAndVersionQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("123456789012:function:my-function", "123", getConfig()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFullArn() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function", null, getConfig()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFullArnWithAlias() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", null, getConfig()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFullArnWithVersion() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", null, getConfig()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFullArnAndAliasQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function", "alias", getConfig()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnFullArnAndVersionQualifier() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function", "123", getConfig()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    @Test
    public void testGetArnDifferentRegion() {
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-west-2:123456789012:function:my-function", null, getConfig()));
        assertEquals("arn:aws:lambda:us-west-2:123456789012:function:my-function", functionProcessedData.getArn());
        assertEquals("my-function", functionProcessedData.getFunctionName());
    }

    private SdkClientConfiguration getConfig() {
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                .option(AwsClientOption.AWS_REGION, Region.US_EAST_1)
                .build();
        return config;
    }
}