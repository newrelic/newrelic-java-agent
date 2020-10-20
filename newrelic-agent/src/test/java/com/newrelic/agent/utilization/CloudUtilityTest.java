/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.Function;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.samplers.SamplerService;
import com.newrelic.agent.stats.IncrementCounter;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWork;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.SocketException;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CloudUtilityTest {

    private StatsService mockStatsService;

    @Test
    public void testLongInvalidValue() {
        CloudUtility target = new CloudUtility();
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            value.append('a');
        }
        assertTrue(target.isInvalidValue(value.toString()));
    }

    @Test
    public void testInvalidCharacters() {
        CloudUtility target = new CloudUtility();
        String invalidCharacters = "*@%$~`\t";
        assertTrue(target.isInvalidValue(invalidCharacters));
    }

    @Test
    public void testInvalidAndValidCharacters() {
        CloudUtility target = new CloudUtility();
        // exclamation mark and delete character at the end, are both invalid characters. Poo is fine, though.
        String invalidAndValid = "Life Is Too Short For \ud83d\udca9 Software !\u007F";
        assertTrue(invalidAndValid.length() <= 255);
        assertTrue(target.isInvalidValue(invalidAndValid));
    }


    @Test
    public void testInvalidNull() {
        assertTrue(new CloudUtility().isInvalidValue(null));
    }

    @Test
    public void testValidEmptyString() {
        // Spec implies so...
        assertFalse(new CloudUtility().isInvalidValue(""));
    }

    @Test
    public void surprisinglyInvalidCharacters() {
        assertTrue(new CloudUtility().isInvalidValue("="));
        assertTrue(new CloudUtility().isInvalidValue(","));
        assertTrue(new CloudUtility().isInvalidValue("\\"));
        assertTrue(new CloudUtility().isInvalidValue("!"));
    }

    @Test
    public void recordsMetric() {
        ArgumentCaptor<StatsWork> captor = ArgumentCaptor.forClass(StatsWork.class);
        doNothing().when(mockStatsService).doStatsWork(captor.capture());

        new CloudUtility().recordError("some error");

        verify(mockStatsService, times(1)).doStatsWork(any(StatsWork.class));

        StatsWork argument = captor.getValue();
        assertTrue(argument instanceof IncrementCounter);
        assertEquals("some error", ((IncrementCounter)argument).getName());
    }

    @Test
    public void testSocketTimeoutExceptionIsHandled() throws Exception {
        Function<Integer, CloseableHttpClient> mockClientCreator = mock(Function.class);
        CloseableHttpClient explodingClient = mock(CloseableHttpClient.class);

        when(mockClientCreator.apply(anyInt())).thenReturn(explodingClient);
        when(explodingClient.execute(isA(HttpUriRequest.class))).thenThrow(new SocketTimeoutException("oof"));

        CloudUtility testClass = new CloudUtility(mockClientCreator);
        String result = testClass.httpGet("https://example.com/some/path", 12, "foo:bar");
        assertNull(result);
    }

    @Test
    public void testSocketExceptionIsHandled() throws Exception {
        Function<Integer, CloseableHttpClient> mockClientCreator = mock(Function.class);
        CloseableHttpClient explodingClient = mock(CloseableHttpClient.class);

        when(mockClientCreator.apply(anyInt())).thenReturn(explodingClient);
        when(explodingClient.execute(isA(HttpUriRequest.class))).thenThrow(new SocketException("oof"));

        CloudUtility testClass = new CloudUtility(mockClientCreator);
        String result = testClass.httpGet("https://example.com/some/path", 12, "foo:bar");
        assertNull(result);
    }

    @Before
    public void before() {
        MockServiceManager mockServiceManager = new MockServiceManager();
        mockServiceManager.setSamplerService(mock(SamplerService.class));
        mockStatsService = mock(StatsService.class);
        mockServiceManager.setStatsService(mockStatsService);
    }
}
