/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk1.services.lambda;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.CloudApi;
import com.newrelic.agent.bridge.NoOpCloud;
import com.newrelic.api.agent.CloudAccountInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class LambdaUtilTest_ProcessData {

    private AWSCredentialsProvider credentialsProvider;

    @Parameterized.Parameter
    public String functionRef;

    @Parameterized.Parameter(1)
    public String qualifier;

    @Parameterized.Parameter(2)
    public boolean shouldMockAccountInfo;

    @Parameterized.Parameter(3)
    public boolean shouldDecodeArn;

    @Parameterized.Parameter(4)
    public String expectedArn;

    @Parameterized.Parameter(5)
    public String expectedFunctionName;

    @Parameterized.Parameters(name="{0}, {1}, {2}, {3}, {4}, {5}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // only function name
                {"my-function",       null,    false, false, "", "my-function"},
                {"my-function",       null,    true,  false, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
                {"my-function",       null,    false, true,  "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
                {"my-function:alias", null,    false, false, "", "my-function"},
                {"my-function:alias", null,    true,  false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", "my-function"},
                {"my-function:123",   null,    false, false, "", "my-function"},
                {"my-function:123",   null,    true,  false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"my-function",       "alias", false, false, "", "my-function"},
                {"my-function",       "alias", true,  false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", "my-function"},
                {"my-function",       "123",   false, false, "", "my-function"},
                {"my-function",       "123",   true,  false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},

                // account and function name partial ARN
                {"123456789012:function:my-function",       null,    false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
                {"123456789012:function:my-function:alias", null,    false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", "my-function"},
                {"123456789012:function:my-function:123",   null,    false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"123456789012:function:my-function",       "alias", false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", "my-function"},
                {"123456789012:function:my-function",       "123",   false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},

                // full arn
                {"arn:aws:lambda:us-east-1:123456789012:function:my-function",       null,    false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
                {"arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", null,    false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", "my-function"},
                {"arn:aws:lambda:us-east-1:123456789012:function:my-function:123",   null,    false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"arn:aws:lambda:us-east-1:123456789012:function:my-function",       "alias", false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:alias", "my-function"},
                {"arn:aws:lambda:us-east-1:123456789012:function:my-function",       "123",   false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"arn:aws:lambda:us-west-2:123456789012:function:my-function",       null,    false, false, "arn:aws:lambda:us-west-2:123456789012:function:my-function", "my-function"},

                // other partial arns
                {"arn::lambda:us-east-1:123456789012:function:my-function:123", null, false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"arn::lambda:us-east-1:123456789012:my-function:123", null, false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"arn::lambda:us-east-1:function:my-function:123", null, true, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"arn::lambda:us-east-1:function:my-function:123", null, false, true, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"arn::lambda:us-east-1:my-function:123", null, true, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"arn::lambda:us-east-1:my-function:123", null, false, true, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"arn:aws:lambda:us-east-1:123456789012:my-function:123", null, false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"arn:aws:lambda:123456789012:function:my-function", null, false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
                {"arn:aws:lambda:123456789012:my-function", null, false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
                {"us-east-1:123456789012:function:my-function:123", null, false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"us-east-1:function:my-function:123", null, true, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"us-east-1:function:my-function:123", null, true, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function:123", "my-function"},
                {"us-east-1:function:my-function:123", null, false, false, "", "my-function"},
                {"us-east-1:function:my-function", null, true, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
                {"us-east-1:function:my-function", null, false, true, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
                {"us-east-1:my-function", null, true, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
                {"us-east-1:my-function", null, false, true, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
                {"us-east-1:my-function", null, false, false, "", "my-function"},
                {"123456789012:my-function", null, false, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},

                // ignore $LATEST
                {"my-function:$LATEST",   null,      true, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
                {"my-function",           "$LATEST", true, false, "arn:aws:lambda:us-east-1:123456789012:function:my-function", "my-function"},
        });
    }

    @Before
    public void before() {
        AgentBridge.cloud = mock(CloudApi.class);
        credentialsProvider = mock(AWSCredentialsProvider.class, RETURNS_DEEP_STUBS);
        when(credentialsProvider.getCredentials().getAWSAccessKeyId()).thenReturn("accessKey");
    }

    @After
    public void after() {
        AgentBridge.cloud = NoOpCloud.INSTANCE;
    }

    @Test
    public void test() {
        if (shouldMockAccountInfo) {
            when(AgentBridge.cloud.getAccountInfo(any(), eq(CloudAccountInfo.AWS_ACCOUNT_ID)))
                    .thenReturn("123456789012");
        }
        if (shouldDecodeArn) {
            when(AgentBridge.cloud.decodeAwsAccountId(anyString()))
                    .thenReturn("123456789012");
        }
        FunctionProcessedData functionProcessedData = LambdaUtil.processData(data(functionRef, qualifier));
        assertEquals(expectedArn, functionProcessedData.getArn());
        assertEquals(expectedFunctionName, functionProcessedData.getFunctionName());
    }

    private FunctionRawData data(String functionRef, String qualifier) {
        return new FunctionRawData(functionRef, qualifier, Regions.US_EAST_1.getName(), this, credentialsProvider);
    }

}