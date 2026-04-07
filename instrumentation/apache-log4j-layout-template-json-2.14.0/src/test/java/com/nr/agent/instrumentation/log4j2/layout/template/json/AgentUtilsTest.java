/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.log4j2.layout.template.json;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class AgentUtilsTest {
    private static final String LOG_JSON_WITH_MESSAGE_FIELD_ESCAPED_QUOTE_COMMA = "{\"instant\" : {\"epochSecond\" : 1734983121,\"nanoOfSecond\" : 537701000},\"level\" : \"INFO\",\"loggerName\" : \"org.hibernate.validator.internal.util.Version\",\"message\" : \"info \\\"bar\\\",\",\"endOfBatch\" : true,\"loggerFqcn\" : \"org.hibernate.validator.internal.util.logging.Log_$logger\"}\n";
    private static final String LOG_JSON_WITH_MESSAGE_LAST_FIELD_ESCAPED_QUOTE_BRACE = "{\"instant\": {\"epochSecond\": 1734983121,\"nanoOfSecond\": 537701000},\"level\": \"INFO\",\"loggerName\": \"org.hibernate.validator.internal.util.Version\",\"endOfBatch\": true,\"loggerFqcn\": \"org.hibernate.validator.internal.util.logging.Log_$logger\",\"message\": \"info \\\"bar\\\",\"}";
    private static final String LOG_JSON_WITH_MESSAGE_FIELD = "{\"instant\" : {\"epochSecond\" : 1734983121,\"nanoOfSecond\" : 537701000},\"level\" : \"INFO\",\"loggerName\" : \"org.hibernate.validator.internal.util.Version\",\"message\" : \"normal msg text\",\"endOfBatch\" : true,\"loggerFqcn\" : \"org.hibernate.validator.internal.util.logging.Log_$logger\"}\n";
    private static final String LOG_JSON_NO_MESSAGE_FIELD = "{\"instant\": {\"epochSecond\": 1734983121,\"nanoOfSecond\": 537701000},\"level\": \"INFO\",\"loggerName\": \"org.hibernate.validator.internal.util.Version\",\"endOfBatch\": true,\"loggerFqcn\": \"org.hibernate.validator.internal.util.logging.Log_$logger\",\"foobar\": \"info \\\"bar\\\",\"}";

    private final Agent originalAgent = AgentBridge.getAgent();
    private final Agent mockAgent = mock(Agent.class);
    private final Logger mockLogger = mock(Logger.class);
    private final Config mockConfig = mock(Config.class);

    private final LogEvent mockLockEvent = mock(LogEvent.class);

    @Before
    public void before() {
        AgentBridge.agent = mockAgent;
        when(mockAgent.getConfig()).thenReturn(mockConfig);
        when(mockAgent.getLogger()).thenReturn(mockLogger);
        when(mockLogger.isLoggable(Level.FINEST)).thenReturn(false);
    }

    @After
    public void after() {
        AgentBridge.agent = originalAgent;
    }

    @Test
    public void getIndexToModifyJson_withMessageField_findsProperIndex() {
        assertEquals(175, AgentUtils.getIndexToModifyJson(LOG_JSON_WITH_MESSAGE_FIELD_ESCAPED_QUOTE_COMMA));
        assertEquals(262, AgentUtils.getIndexToModifyJson(LOG_JSON_WITH_MESSAGE_LAST_FIELD_ESCAPED_QUOTE_BRACE));
    }

    @Test
    public void getIndexToModifyJson_withNoMessageField_returnsNegativeOne() {
        assertEquals(-1, AgentUtils.getIndexToModifyJson(LOG_JSON_NO_MESSAGE_FIELD));
    }

    @Test
    public void writeLinkingMetadata_addsMetadataProperly() {
        when(mockConfig.getValue(anyString(), anyBoolean())).thenReturn(true);

        StringBuilder sb = new StringBuilder(LOG_JSON_WITH_MESSAGE_FIELD_ESCAPED_QUOTE_COMMA);
        AgentUtils.writeLinkingMetadata(mockLockEvent, sb);
        assertTrue(sb.toString().contains("\"message\" : \"info \\\"bar\\\", NR-LINKING|\""));

        sb = new StringBuilder(LOG_JSON_WITH_MESSAGE_LAST_FIELD_ESCAPED_QUOTE_BRACE);
        AgentUtils.writeLinkingMetadata(mockLockEvent, sb);
        assertTrue(sb.toString().contains("\"message\": \"info \\\"bar\\\", NR-LINKING|\"}"));

        sb = new StringBuilder(LOG_JSON_WITH_MESSAGE_FIELD);
        AgentUtils.writeLinkingMetadata(mockLockEvent, sb);
        assertTrue(sb.toString().contains("\"message\" : \"normal msg text NR-LINKING|\""));

        sb = new StringBuilder(LOG_JSON_NO_MESSAGE_FIELD);
        AgentUtils.writeLinkingMetadata(mockLockEvent, sb);
        assertFalse(sb.toString().contains("NR-LINKING|"));

    }

    // Leaving these next two methods here in case anyone wants to run a simple performance test of the regex matching.
    // Simple uncomment the @Test annotation and run like a normal test.
    //@Test
    public void simplePerfTest() {
        final String JSON_TEMPLATE = "{\"instant\" : {\"epochSecond\" : 1734983121,\"nanoOfSecond\" : 537701000},\"level\" : \"INFO\",\"loggerName\" : \"org.hibernate.validator.internal.util.Version\",\"message\" : \"{MSG_VAL}\",\"endOfBatch\" : true,\"loggerFqcn\" : \"org.hibernate.validator.internal.util.logging.Log_$logger\"}\n";
        final String ESCAPED_QUOTE_COMMA = "info \\\"bar\\\",";
        final int LOOP_SIZE = 1000000;
        Random random = new Random();
        List<String> randomStringList = new ArrayList<>();

        // Gen up a list of random Strings so we don't spend cycles in the actual timing loop creating them
        for (int i=0; i<LOOP_SIZE; i++) {
            randomStringList.add(generateRandomStr(10));
        }

        long start = System.currentTimeMillis();
        for (int i=0; i<LOOP_SIZE; i++) {
            if (random.nextInt(101) >= 90) {
                AgentUtils.getIndexToModifyJson(JSON_TEMPLATE.replace("{MSG_VAL}", ESCAPED_QUOTE_COMMA));
            } else {
                AgentUtils.getIndexToModifyJson(JSON_TEMPLATE.replace("{MSG_VAL}", randomStringList.get(i)));
            }
        }
        System.out.println("Time ---> " + (System.currentTimeMillis() - start));
    }

    private static String generateRandomStr(int length) {
        String CANDIDATE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i ++) {
            sb.append(CANDIDATE_CHARS.charAt(random.nextInt(26)));
        }

        return sb.toString();
    }
}
