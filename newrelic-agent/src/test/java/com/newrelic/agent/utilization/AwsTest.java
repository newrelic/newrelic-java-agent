/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.utilization.AWS.AwsData;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link AWS} cloud vendor class.
 */
public class AwsTest {

    @Test
    public void testAwsValueMap() throws IOException {
        CloudUtility mockCloud = mock(CloudUtility.class);
        when(mockCloud.isInvalidValue(anyString())).thenReturn(false);
        AWS aws = new AWS(mockCloud);

        when(mockCloud.httpPut(anyString(), anyInt(), anyString())).thenReturn("some-token");
        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn("{"
                + "\"instanceType\" : \"test.type\","
                + "\"instanceId\" : \"test.id\","
                + "\"availabilityZone\" : \"us-west-2b\","
                + "}"
        );

        AwsData awsData = aws.getData();
        assertFalse(awsData.getValueMap().isEmpty());
        verify(mockCloud, never()).recordError("Supportability/utilization/aws/error");
    }


    @Test
    public void doesTheTokenDance() throws IOException {
        CloudUtility mockCloud = mock(CloudUtility.class);
        when(mockCloud.isInvalidValue(anyString())).thenReturn(false);
        AWS aws = new AWS(mockCloud);

        ArgumentCaptor<String> tokenHeaderCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> requestHeaderCaptor = ArgumentCaptor.forClass(String.class);

        when(mockCloud.httpPut(anyString(), anyInt(), tokenHeaderCaptor.capture())).thenReturn("some-token");
        when(mockCloud.httpGet(anyString(), anyInt(), requestHeaderCaptor.capture())).thenReturn("{"
                + "\"instanceType\" : \"test.type\","
                + "\"instanceId\" : \"test.id\","
                + "\"availabilityZone\" : \"us-west-2b\","
                + "}"
        );

        aws.getData();

        verify(mockCloud, never()).recordError("Supportability/utilization/aws/error");
        assertTrue(tokenHeaderCaptor.getValue().startsWith("X-aws-ec2-metadata-token-ttl-seconds:"));
        assertEquals("X-aws-ec2-metadata-token: some-token", requestHeaderCaptor.getValue());
    }

    @Test
    public void crossAgentTestTimesOut() throws IOException {
        // "testname": "aws api times out, no vendor hash or supportability metric reported"
        CloudUtility mockCloud = mock(CloudUtility.class);

        AWS aws = new AWS(mockCloud);
        when(mockCloud.httpPut(anyString(), anyInt(), anyString())).thenReturn("some-token");
        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn(null);

        AwsData awsData = aws.getData();
        assertEquals(AwsData.EMPTY_DATA, awsData);

        verify(mockCloud, never()).recordError("Supportability/utilization/aws/error");
    }

    @Test
    public void crossAgentTest() throws IOException {
        // "testname":"instance type, instance-id, availability-zone are all happy"
        CloudUtility mockCloud = mock(CloudUtility.class);
        AWS aws = new AWS(mockCloud);

        when(mockCloud.httpPut(anyString(), anyInt(), anyString())).thenReturn("some-token");
        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn("{"
                + "\"availabilityZone\" : \"us-west-2b\","
                + "\"instanceId\" : \"test.id\","
                + "\"instanceType\" : \"test.type\","
                + "}");

        AwsData awsData = aws.getData();
        assertEquals("test.type", awsData.getInstanceType());
        assertEquals("test.id", awsData.getInstanceId());
        assertEquals("us-west-2b", awsData.getAvailabilityZone());
    }

    @Test
    public void crossAgentTestUtfCodepoints() throws IOException {
        // "testname": "UTF-8 high codepoints"
        CloudUtility mockCloud = mock(CloudUtility.class);
        AWS aws = new AWS(mockCloud);

        when(mockCloud.httpPut(anyString(), anyInt(), anyString())).thenReturn("some-token");
        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn("{"
                + "\"availabilityZone\" : \"us-west-2b\","
                + "\"instanceId\" : \"滈 橀槶澉 鞻饙騴 鱙鷭黂 甗糲 紁羑 嗂 蛶觢豥 餤駰鬳 釂鱞鸄\","
                + "\"instanceType\" : \"test.type\","
                + "}");

        AwsData awsData = aws.getData();
        assertEquals("test.type", awsData.getInstanceType());
        assertEquals("滈 橀槶澉 鞻饙騴 鱙鷭黂 甗糲 紁羑 嗂 蛶觢豥 餤駰鬳 釂鱞鸄", awsData.getInstanceId());
        assertEquals("us-west-2b", awsData.getAvailabilityZone());
    }

    @Test
    public void returnsEmptyDataIfAnyValueInvalid() throws IOException {
        // "testname": "Exclamation point response"
        CloudUtility mockCloud = mock(CloudUtility.class);
        when(mockCloud.isInvalidValue("bang!")).thenReturn(true);
        AWS aws = new AWS(mockCloud);

        when(mockCloud.httpPut(anyString(), anyInt(), anyString())).thenReturn("some-token");
        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn("{"
                + "\"availabilityZone\" : \"us-west-2b\","
                + "\"instanceType\" : \"test.type\","
                + "\"instanceId\" : \"bang!\","
                + "}");

        AwsData awsData = aws.getData();
        assertEquals(AwsData.EMPTY_DATA, awsData);

        verify(mockCloud).recordError("Supportability/utilization/aws/error");
    }

    @Test
    public void returnsEmptyDataAndErrorOnOtherException() throws IOException {
        CloudUtility mockCloud = mock(CloudUtility.class);
        AWS aws = new AWS(mockCloud);

        when(mockCloud.httpPut(anyString(), anyInt(), anyString())).thenThrow(new RuntimeException());

        AwsData awsData = aws.getData();
        assertEquals(AwsData.EMPTY_DATA, awsData);

        verify(mockCloud, times(1)).httpPut(anyString(), anyInt(), anyString());
        verify(mockCloud).recordError("Supportability/utilization/aws/error");
        verifyNoMoreInteractions(mockCloud);
    }
}
