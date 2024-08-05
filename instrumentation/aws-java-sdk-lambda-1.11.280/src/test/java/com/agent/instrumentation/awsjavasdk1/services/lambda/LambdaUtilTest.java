/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk1.services.lambda;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.newrelic.api.agent.CloudParameters;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class LambdaUtilTest {

    @Test
    public void testGetCloudParamFunctionName() {
        FunctionRawData functionRawData = new FunctionRawData("my-function", null, getRegion());
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        assertNotNull(cloudParameters);
        assertEquals("aws_lambda", cloudParameters.getPlatform());
        assertNull(cloudParameters.getResourceId());
    }

    @Test
    public void testGetCloudParamPartialArn() {
        FunctionRawData functionRawData = new FunctionRawData("123456789012:function:my-function", null, getRegion());
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        assertNotNull(cloudParameters);
        assertEquals("aws_lambda", cloudParameters.getPlatform());
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", cloudParameters.getResourceId());
    }

    @Test
    public void testGetCloudParamArnQualifier() {
        FunctionRawData functionRawData = new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function", "alias", getRegion());
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        assertNotNull(cloudParameters);
        assertEquals("aws_lambda", cloudParameters.getPlatform());
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", cloudParameters.getResourceId());
    }

    @Test
    public void testGetArnFunctionName() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("my-function", null, getRegion()));
        assertEquals("", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithAlias() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("my-function:alias", null, getRegion()));
        assertEquals("", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameWithVersion() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("my-function:123", null, getRegion()));
        assertEquals("", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndAliasQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("my-function", "alias", getRegion()));
        assertEquals("", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFunctionNameAndVersionQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("my-function", "123", getRegion()));
        assertEquals("", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnPartialArn() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("123456789012:function:my-function", null, getRegion()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnWithAlias() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("123456789012:function:my-function:alias", null, getRegion()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnWithVersion() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("123456789012:function:my-function:123", null, getRegion()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnAndAliasQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("123456789012:function:my-function", "alias", getRegion()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnPartialArnAndVersionQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("123456789012:function:my-function", "123", getRegion()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFullArn() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function", null, getRegion()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFullArnWithAlias() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", null, getRegion()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFullArnWithVersion() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function:123", null, getRegion()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFullArnAndAliasQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function", "alias", getRegion()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnFullArnAndVersionQualifier() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-east-1:123456789012:function:my-function", "123", getRegion()));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    @Test
    public void testGetArnDifferentRegion() {
        FunctionProcessedData data = LambdaUtil.processData(new FunctionRawData("arn:aws:lambda:us-west-2:123456789012:function:my-function", null, getRegion()));
        assertEquals("arn:aws:lambda:us-west-2:123456789012:function:my-function", data.getArn());
        assertEquals("my-function", data.getFunctionName());
    }

    public String getRegion() {
        return Regions.US_EAST_1.getName();
    }

}