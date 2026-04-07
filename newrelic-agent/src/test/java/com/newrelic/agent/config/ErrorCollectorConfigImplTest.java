/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.Iterables;
import com.newrelic.agent.Mocks;
import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ErrorCollectorConfigImplTest {

    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    protected Map<String, Object> createMap() {
        Map<String, Object> configMap = new HashMap<>();
        return configMap;
    }

    @Test
    public void isEnabled() throws Exception {
        Map<String, Object> localSettings = createMap();
        localSettings.put("enabled", !ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(false, config.isEnabled());
    }

    @Test
    public void isEnabledServer() throws Exception {
        Map<String, Object> serverSettings = createMap();
        serverSettings.put(ErrorCollectorConfigImpl.ENABLED, !ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        serverSettings.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(serverSettings);

        assertEquals(!ErrorCollectorConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isEnabledServerNotCollectErrors() throws Exception {
        Map<String, Object> serverSettings = createMap();
        serverSettings.put(ErrorCollectorConfigImpl.ENABLED, true);
        serverSettings.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, false);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(serverSettings);

        assertEquals(false, config.isEnabled());
    }

    @Test
    public void isEnabledServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = ErrorCollectorConfigImpl.SYSTEM_PROPERTY_ROOT + ErrorCollectorConfigImpl.ENABLED;
        String val = String.valueOf(!ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = createMap();
        ServerProp serverProp = ServerProp.createPropObject(!ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        serverSettings.put(ErrorCollectorConfigImpl.ENABLED, serverProp);
        serverSettings.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(serverSettings);

        assertEquals(!ErrorCollectorConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isEnabledSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = ErrorCollectorConfigImpl.SYSTEM_PROPERTY_ROOT + ErrorCollectorConfigImpl.ENABLED;
        String val = String.valueOf(!ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = createMap();
        localSettings.put(ErrorCollectorConfigImpl.ENABLED, ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(false, config.isEnabled());
    }

    @Test
    public void isEnabledDefault() throws Exception {
        Map<String, Object> localSettings = createMap();
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(ErrorCollectorConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void ignoreErrors() throws Exception {
        Map<String, Object> localSettings = createMap();
        List<String> ignoreErrors = new ArrayList<>();
        ignoreErrors.add("java.lang.Exception");
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_ERRORS, ignoreErrors);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(1, config.getIgnoreErrors().size());
        verifyIgnoredErrorClassAndMessage(config, "java.lang.Exception", null);
    }

    @Test
    public void ignoreErrorsDefault() throws Exception {
        Map<String, Object> localSettings = createMap();
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);
        assertEquals(ErrorCollectorConfigImpl.DEFAULT_IGNORE_ERRORS.size(), config.getIgnoreErrors().size());
        for (String defaultIgnores : ErrorCollectorConfigImpl.DEFAULT_IGNORE_ERRORS) {
            verifyIgnoredErrorClassAndMessage(config, defaultIgnores, null);
        }
    }

    @Test
    public void ignoreErrorsMixedNotation() throws Exception {
        Map<String, Object> localSettings = createMap();
        List<String> ignoreErrors = new ArrayList<>();
        ignoreErrors.add("java.lang.Exception");
        ignoreErrors.add("org/eclipse/jetty/continuation/ContinuationThrowable");
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_ERRORS, ignoreErrors);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(2, config.getIgnoreErrors().size());
        verifyIgnoredErrorClassAndMessage(config, "java.lang.Exception", null);
        verifyIgnoredErrorClassAndMessage(config, "org.eclipse.jetty.continuation.ContinuationThrowable", null);
    }

    @Test
    public void ignoreErrorsServer() throws Exception {
        Map<String, Object> serverSettings = createMap();
        List<String> ignoreErrors = new ArrayList<>();
        ignoreErrors.add("java.lang.Exception");
        serverSettings.put(ErrorCollectorConfigImpl.IGNORE_ERRORS, ignoreErrors);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(serverSettings);

        assertEquals(1, config.getIgnoreErrors().size());
        verifyIgnoredErrorClassAndMessage(config, "java.lang.Exception", null);
    }

    @Test
    public void ignoreErrorsServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = ErrorCollectorConfigImpl.SYSTEM_PROPERTY_ROOT + ErrorCollectorConfigImpl.IGNORE_ERRORS;
        String val = String.valueOf("java.lang.Exception, java.io.IOException");
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = createMap();
        List<String> ignoreErrors = new ArrayList<>();
        ignoreErrors.add("java.lang.UnsupportedOperationException");
        ignoreErrors.add("java.lang.ArrayIndexOutOfBoundsException");
        ServerProp serverProp = ServerProp.createPropObject(ignoreErrors);
        serverSettings.put(ErrorCollectorConfigImpl.IGNORE_ERRORS, serverProp);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(serverSettings);

        assertEquals(2, config.getIgnoreErrors().size());
        verifyIgnoredErrorClassAndMessage(config, "java.lang.UnsupportedOperationException", null);
        verifyIgnoredErrorClassAndMessage(config, "java.lang.ArrayIndexOutOfBoundsException", null);
    }

    @Test
    public void ignoreErrorsSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = ErrorCollectorConfigImpl.SYSTEM_PROPERTY_ROOT + ErrorCollectorConfigImpl.IGNORE_ERRORS;
        String val = String.valueOf("java.lang.Exception, java.io.IOException");
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = createMap();
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(2, config.getIgnoreErrors().size());
        verifyIgnoredErrorClassAndMessage(config, "java.lang.Exception", null);
        verifyIgnoredErrorClassAndMessage(config, "java.io.IOException", null);
    }

    @Test
    public void ignoreErrorsMultiple() throws Exception {
        Map<String, Object> localSettings = createMap();
        List<String> ignoreErrors = new ArrayList<>();
        ignoreErrors.add("java.lang.Exception");
        ignoreErrors.add("java.io.IOException");
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_ERRORS, ignoreErrors);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(2, config.getIgnoreErrors().size());
        verifyIgnoredErrorClassAndMessage(config, "java.lang.Exception", null);
        verifyIgnoredErrorClassAndMessage(config, "java.io.IOException", null);
    }

    @Test
    public void ignoreErrorsDuplicates() throws Exception {
        Map<String, Object> localSettings = createMap();
        List<String> ignoreErrors = new ArrayList<>();
        ignoreErrors.add("java.lang.Exception");
        ignoreErrors.add("java.io.IOException");
        ignoreErrors.add("java.lang.Exception");
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_ERRORS, ignoreErrors);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(2, config.getIgnoreErrors().size());
        verifyIgnoredErrorClassAndMessage(config, "java.lang.Exception", null);
        verifyIgnoredErrorClassAndMessage(config, "java.io.IOException", null);
    }

    @Test
    public void ignoreErrorsString() throws Exception {
        Map<String, Object> localSettings = createMap();
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_ERRORS, "java.lang.Exception");
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(1, config.getIgnoreErrors().size());
        verifyIgnoredErrorClassAndMessage(config, "java.lang.Exception", null);
    }

    @Test
    public void ignoreErrorsMultipleString() throws Exception {
        Map<String, Object> localSettings = createMap();
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_ERRORS, "java.lang.Exception, java.io.IOException");
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(2, config.getIgnoreErrors().size());
        verifyIgnoredErrorClassAndMessage(config, "java.lang.Exception", null);
        verifyIgnoredErrorClassAndMessage(config, "java.io.IOException", null);
    }

    @Test
    public void ignoreErrorsDuplicatesString() throws Exception {
        Map<String, Object> localSettings = createMap();
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_ERRORS,
                "java.lang.Exception, java.io.IOException, java.lang.Exception");
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(2, config.getIgnoreErrors().size());
        verifyIgnoredErrorClassAndMessage(config, "java.lang.Exception", null);
        verifyIgnoredErrorClassAndMessage(config, "java.io.IOException", null);
    }

    @Test
    public void ignoreStatusCodes() throws Exception {
        Map<String, Object> localSettings = createMap();
        List<Integer> ignoreStatusCodes = new ArrayList<>();
        ignoreStatusCodes.add(503);
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, ignoreStatusCodes);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(1, config.getIgnoreStatusCodes().size());
        assertTrue(config.getIgnoreStatusCodes().contains(503));
    }

    @Test
    public void ignoreStatusCodesServer() throws Exception {
        Map<String, Object> serverSettings = createMap();
        List<Integer> ignoreStatusCodes = new ArrayList<>();
        ignoreStatusCodes.add(503);
        serverSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, ignoreStatusCodes);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(serverSettings);

        assertEquals(1, config.getIgnoreStatusCodes().size());
        assertTrue(config.getIgnoreStatusCodes().contains(503));
    }

    @Test
    public void ignoreStatusCodesDefault() throws Exception {
        Map<String, Object> localSettings = createMap();
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(ErrorCollectorConfigImpl.DEFAULT_IGNORE_STATUS_CODES, config.getIgnoreStatusCodes());
    }

    @Test
    public void ignoreStatusCodesMultiple() throws Exception {
        Map<String, Object> localSettings = createMap();
        List<Integer> ignoreStatusCodes = new ArrayList<>();
        ignoreStatusCodes.add(503);
        ignoreStatusCodes.add(502);
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, ignoreStatusCodes);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(2, config.getIgnoreStatusCodes().size());
        assertTrue(config.getIgnoreStatusCodes().contains(502));
        assertTrue(config.getIgnoreStatusCodes().contains(503));
    }

    @Test
    public void ignoreStatusCodesServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = ErrorCollectorConfigImpl.SYSTEM_PROPERTY_ROOT + ErrorCollectorConfigImpl.IGNORE_STATUS_CODES;
        String val = String.valueOf("504, 505");
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = createMap();
        List<Integer> ignoreStatusCodes = new ArrayList<>();
        ignoreStatusCodes.add(503);
        ignoreStatusCodes.add(502);
        ServerProp serverProp = ServerProp.createPropObject(ignoreStatusCodes);
        serverSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, serverProp);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(serverSettings);

        assertEquals(2, config.getIgnoreStatusCodes().size());
        assertTrue(config.getIgnoreStatusCodes().contains(502));
        assertTrue(config.getIgnoreStatusCodes().contains(503));
    }


    @Test
    public void ignoreStatusCodesSystemPropertySingleCode() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = ErrorCollectorConfigImpl.SYSTEM_PROPERTY_ROOT + ErrorCollectorConfigImpl.IGNORE_STATUS_CODES;
        String val = String.valueOf("503");
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = createMap();
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(1, config.getIgnoreStatusCodes().size());
        assertTrue(config.getIgnoreStatusCodes().contains(503));
    }

    @Test
    public void ignoreStatusCodesSystemPropertyMultipleCodes() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = ErrorCollectorConfigImpl.SYSTEM_PROPERTY_ROOT + ErrorCollectorConfigImpl.IGNORE_STATUS_CODES;
        String val = String.valueOf("503, 502");
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = createMap();
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(2, config.getIgnoreStatusCodes().size());
        assertTrue(config.getIgnoreStatusCodes().contains(502));
        assertTrue(config.getIgnoreStatusCodes().contains(503));
    }

    @Test
    public void ignoreStatusCodesDuplicates() throws Exception {
        Map<String, Object> localSettings = createMap();
        List<Integer> ignoreStatusCodes = new ArrayList<>();
        ignoreStatusCodes.add(503);
        ignoreStatusCodes.add(502);
        ignoreStatusCodes.add(503);
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, ignoreStatusCodes);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(2, config.getIgnoreStatusCodes().size());
        assertTrue(config.getIgnoreStatusCodes().contains(502));
        assertTrue(config.getIgnoreStatusCodes().contains(503));
    }

    @Test
    public void ignoreStatusCodesNumber() throws Exception {
        Map<String, Object> localSettings = createMap();
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, 503);
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(1, config.getIgnoreStatusCodes().size());
        assertTrue(config.getIgnoreStatusCodes().contains(503));
    }

    @Test
    public void ignoreStatusCodesString() throws Exception {
        Map<String, Object> localSettings = createMap();
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, "503");
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(1, config.getIgnoreStatusCodes().size());
        assertTrue(config.getIgnoreStatusCodes().contains(503));
    }

    @Test
    public void ignoreStatusCodesMultipleString() throws Exception {
        Map<String, Object> localSettings = createMap();
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, "502, 503");
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(2, config.getIgnoreStatusCodes().size());
        assertTrue(config.getIgnoreStatusCodes().contains(502));
        assertTrue(config.getIgnoreStatusCodes().contains(503));
    }

    @Test
    public void ignoreStatusCodesDuplicatesString() throws Exception {
        Map<String, Object> localSettings = createMap();
        localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, "502, 503, 502");
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertEquals(2, config.getIgnoreStatusCodes().size());
        assertTrue(config.getIgnoreStatusCodes().contains(502));
        assertTrue(config.getIgnoreStatusCodes().contains(503));
    }

    @Test
    public void emptyExpectedErrors() throws Exception {
        Map<String, Object> localSettings = createMap();
        localSettings.put(ErrorCollectorConfigImpl.EXPECTED_CLASSES, new ArrayList<Map<String, String>>());
        ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

        assertTrue(config.getExpectedErrors().isEmpty());
        assertTrue(config.getExpectedStatusCodes().isEmpty());
    }

    @Test
    public void bogusExpectedErrors() throws Exception {
        LogMocker mocker = new LogMocker();
        try {
            Map<String, Object> localSettings = createMap();
            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_CLASSES, 403);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            mocker.verifyMessage(Level.WARNING);
            Assert.assertTrue(config.getExpectedErrors().isEmpty());
            Assert.assertTrue(config.getExpectedStatusCodes().isEmpty());
        } finally {
            mocker.close();
        }
    }


    @Test
    public void singleExpectedErrorsClass() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String expectedErrorClass = "com.newrelic.ExpectedErrorClass";

            Map<String, Object> localSettings = createMap();
            List<Map<String, String>> expectedErrorsList = new ArrayList<>();
            Map<String, String> expectedErrorMap = new HashMap<>();
            expectedErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, expectedErrorClass);
            expectedErrorsList.add(expectedErrorMap);

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_CLASSES, expectedErrorsList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<ExpectedErrorConfig> expectedErrors = config.getExpectedErrors();
            assertFalse(expectedErrors.isEmpty());
            assertEquals(1, expectedErrors.size());
            ExpectedErrorConfig expectedError = Iterables.get(expectedErrors, 0);
            assertNotNull(expectedError);
            assertEquals(expectedErrorClass, expectedError.getErrorClass());
            Assert.assertNull(expectedError.getErrorMessage());

            assertTrue(config.getExpectedStatusCodes().isEmpty());
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void multipleExpectedErrorsClass() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String expectedErrorClass = "com.newrelic.ExpectedErrorClass";
            String otherExpectedErrorClass = "com.newrelic.OtherExpectedErrorClass";

            Map<String, Object> localSettings = createMap();
            List<Map<String, String>> expectedErrorsList = new ArrayList<>();
            Map<String, String> expectedErrorMap = new HashMap<>();
            Map<String, String> otherExpectedErrorMap = new HashMap<>();
            expectedErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, expectedErrorClass);
            otherExpectedErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, otherExpectedErrorClass);
            expectedErrorsList.add(expectedErrorMap);
            expectedErrorsList.add(otherExpectedErrorMap);

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_CLASSES, expectedErrorsList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<ExpectedErrorConfig> expectedErrors = config.getExpectedErrors();
            assertFalse(expectedErrors.isEmpty());
            assertEquals(2, expectedErrors.size());

            ExpectedErrorConfig expectedError = Iterables.get(expectedErrors, 0);
            assertNotNull(expectedError);
            assertEquals(otherExpectedErrorClass, expectedError.getErrorClass());
            Assert.assertNull(expectedError.getErrorMessage());

            assertTrue(config.getExpectedStatusCodes().isEmpty());
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void expectedErrorsClassMessage() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String expectedErrorClass = "com.newrelic.ExpectedErrorClass";
            String otherExpectedErrorClass = "com.newrelic.OtherExpectedErrorClass";
            String expectedErrorMessage = "This error is expected";

            Map<String, Object> localSettings = createMap();
            List<Map<String, String>> expectedErrorsList = new ArrayList<>();

            Map<String, String> expectedErrorMap = new HashMap<>();
            expectedErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, expectedErrorClass);
            expectedErrorMap.put(ErrorCollectorConfigImpl.MESSAGE, expectedErrorMessage);

            Map<String, String> otherExpectedErrorMap = new HashMap<>();
            otherExpectedErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, otherExpectedErrorClass);
            expectedErrorsList.add(expectedErrorMap);
            expectedErrorsList.add(otherExpectedErrorMap);

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_CLASSES, expectedErrorsList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<ExpectedErrorConfig> expectedErrors = config.getExpectedErrors();
            assertFalse(expectedErrors.isEmpty());
            assertEquals(2, expectedErrors.size());

            ExpectedErrorConfig expectedError = Iterables.get(expectedErrors, 1);

            assertNotNull(expectedError);
            assertEquals(expectedErrorClass, expectedError.getErrorClass());
            assertEquals(expectedError.getErrorMessage(), expectedErrorMessage);

            assertTrue(config.getExpectedStatusCodes().isEmpty());
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void expectedErrorsStatusCodesDuplicates() throws Exception {
        LogMocker mocker = new LogMocker();
        try {

            String expectedStatusCode = "502,502,,502";

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_STATUS_CODES, expectedStatusCode);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            // For the expectedStatusCode above, we ignore the ",," and move on - no warning.
            mocker.verifyNoMessage();
            Assert.assertFalse(config.getExpectedStatusCodes().isEmpty());
            Assert.assertEquals(1, config.getExpectedStatusCodes().size());
        } finally {
            mocker.close();
        }
    }

    @Test
    public void expectedErrorsStatusCodesRange() throws Exception {
        LogMocker mocker = new LogMocker();
        try {

            String expectedStatusCode = "500-509,";

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_STATUS_CODES, expectedStatusCode);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            // We ignore the extra comma in the String above - no warning.
            mocker.verifyNoMessage();
            Assert.assertFalse(config.getExpectedStatusCodes().isEmpty());
            Assert.assertEquals(10, config.getExpectedStatusCodes().size());
        } finally {
            mocker.close();
        }
    }

    @Test
    public void expectedErrorsStatusCodesInvalidDownwardRange() throws Exception {
        LogMocker mocker = new LogMocker();
        try {

            String expectedStatusCode = "502-402";

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_STATUS_CODES, expectedStatusCode);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            mocker.verifyMessage(Level.WARNING);
            Assert.assertTrue(config.getExpectedStatusCodes().isEmpty());
        } finally {
            mocker.close();
        }
    }

    @Test
    public void expectedErrorsStatusCodesInvalidSizeRange() throws Exception {
        LogMocker mocker = new LogMocker();
        try {

            String expectedStatusCode = "1-1001";

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_STATUS_CODES, expectedStatusCode);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            mocker.verifyMessage(Level.WARNING);
            Assert.assertTrue(config.getExpectedStatusCodes().isEmpty());
        } finally {
            mocker.close();
        }
    }

    @Test
    public void expectedErrorsStatusCodesRangeUnbounded() throws Exception {
        LogMocker mocker = new LogMocker();
        try {

            String expectedStatusCode = "400-";

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_STATUS_CODES, expectedStatusCode);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            mocker.verifyMessage(Level.WARNING);
            Assert.assertTrue(config.getExpectedStatusCodes().isEmpty());
        } finally {
            mocker.close();
        }
    }

    @Test
    public void expectedErrorsStatusCodesInt() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            int expectedStatusCode = 400;

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_STATUS_CODES, expectedStatusCode);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertEquals(config.getExpectedStatusCodes().size(), 1);
            assertTrue(config.getExpectedStatusCodes().contains(expectedStatusCode));
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void expectedErrorsMessageDuplicate() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String expectedErrorClass = "com.newrelic.ExpectedErrorClass";
            String expectedErrorMessage = "dude";
            String duplicateErrorMessage = "edud";

            Map<String, Object> localSettings = createMap();

            Map<String, String> expectedErrorMap = new HashMap<>();
            expectedErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, expectedErrorClass);
            expectedErrorMap.put(ErrorCollectorConfigImpl.MESSAGE, expectedErrorMessage);
            expectedErrorMap.put(ErrorCollectorConfigImpl.MESSAGE, duplicateErrorMessage);

            List<Map<String, String>> expectedErrorsList = new ArrayList<>();
            expectedErrorsList.add(expectedErrorMap);

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_CLASSES, expectedErrorsList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<ExpectedErrorConfig> expectedErrors = config.getExpectedErrors();
            assertFalse(expectedErrors.isEmpty());
            ExpectedErrorConfig expectedErrorConfig = Iterables.get(expectedErrors, 0);
            assertNotNull(expectedErrorConfig);
            assertEquals(expectedErrorClass, expectedErrorConfig.getErrorClass());
            assertEquals(duplicateErrorMessage, expectedErrorConfig.getErrorMessage()); // last one wins (it's a map)
            assertTrue(config.getExpectedStatusCodes().isEmpty());
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void emptyIgnoreErrors() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {

            Map<String, Object> localSettings = createMap();
            localSettings.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, new ArrayList<Map<String, String>>());
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertFalse(config.getIgnoreErrors().isEmpty());
            assertEquals(1, config.getIgnoreErrors().size());
            IgnoreErrorConfig ignoreError = Iterables.get(config.getIgnoreErrors(), 0);
            assertNotNull(ignoreError);
            assertEquals("akka.actor.ActorKilledException", ignoreError.getErrorClass());
            Assert.assertNull(ignoreError.getErrorMessage());

            assertEquals(1, config.getIgnoreStatusCodes().size());
            assertTrue(config.getIgnoreStatusCodes().contains(404));
            assertTrue(config.getExpectedErrors().isEmpty());
            assertTrue(config.getExpectedStatusCodes().isEmpty());
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void singleIgnoreErrorsClass() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String ignoreErrorClass = "com.newrelic.IgnoreErrorClass";

            Map<String, Object> localSettings = createMap();
            List<Map<String, String>> ignoreErrorsList = new ArrayList<>();
            Map<String, String> ignoreErrorMap = new HashMap<>();
            ignoreErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, ignoreErrorClass);
            ignoreErrorsList.add(ignoreErrorMap);

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreErrorsList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<ExpectedErrorConfig> expectedErrors = config.getExpectedErrors();
            assertTrue(expectedErrors.isEmpty());
            assertTrue(config.getExpectedStatusCodes().isEmpty());
            logMocker.verifyNoMessage();

            Set<IgnoreErrorConfig> ignoreErrors = config.getIgnoreErrors();
            assertFalse(ignoreErrors.isEmpty());
            assertEquals(1, ignoreErrors.size());
            IgnoreErrorConfig ignoreError = Iterables.get(ignoreErrors, 0);
            assertNotNull(ignoreError);
            assertEquals(ignoreErrorClass, ignoreError.getErrorClass());
            Assert.assertNull(ignoreError.getErrorMessage());

            assertEquals(1, config.getIgnoreStatusCodes().size());
            assertTrue(config.getIgnoreStatusCodes().contains(404));
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void multipleIgnoreErrorsClass() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String ignoreErrorClass = "com.newrelic.IgnoreErrorClass";
            String otherIgnoreErrorClass = "com.newrelic.OtherIgnoreErrorClass";

            Map<String, Object> localSettings = createMap();
            List<Map<String, String>> ignoreErrorsList = new ArrayList<>();
            Map<String, String> ignoreErrorMap = new HashMap<>();
            Map<String, String> otherIgnoreErrorMap = new HashMap<>();
            ignoreErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, ignoreErrorClass);
            otherIgnoreErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, otherIgnoreErrorClass);
            ignoreErrorsList.add(ignoreErrorMap);
            ignoreErrorsList.add(otherIgnoreErrorMap);

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreErrorsList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertTrue(config.getExpectedErrors().isEmpty());
            assertTrue(config.getExpectedStatusCodes().isEmpty());

            Set<IgnoreErrorConfig> ignoreErrors = config.getIgnoreErrors();
            assertFalse(ignoreErrors.isEmpty());
            assertEquals(2, ignoreErrors.size());
            logMocker.verifyNoMessage();

            for (IgnoreErrorConfig ignoreError : ignoreErrors) {
                assertNotNull(ignoreError);
                if (ignoreError.getErrorClass().equals(ignoreErrorClass)) {
                    assertEquals(ignoreErrorClass, ignoreError.getErrorClass());
                    assertNull(ignoreError.getErrorMessage());
                    assertEquals(1, config.getIgnoreStatusCodes().size());
                    assertTrue(config.getIgnoreStatusCodes().contains(404));
                } else if (ignoreError.getErrorClass().equals(otherIgnoreErrorClass)) {
                    assertEquals(otherIgnoreErrorClass, ignoreError.getErrorClass());
                    assertNull(ignoreError.getErrorMessage());
                    assertEquals(1, config.getIgnoreStatusCodes().size());
                    assertTrue(config.getIgnoreStatusCodes().contains(404));
                } else {
                    fail("Unexpected error found");
                }
            }
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void ignoreErrorsClassMessage() throws Exception {
        LogMocker logMocker = new LogMocker();

        try {
            String ignoreErrorClass = "com.newrelic.IgnoreErrorClass";
            String otherIgnoreErrorClass = "com.newrelic.OtherIgnoreErrorClass";
            String ignoreErrorMessage = "This error is ignored";

            Map<String, Object> localSettings = createMap();
            List<Map<String, String>> ignoreErrorsList = new ArrayList<>();

            Map<String, String> ignoreErrorMap = new HashMap<>();
            ignoreErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, ignoreErrorClass);
            ignoreErrorMap.put(ErrorCollectorConfigImpl.MESSAGE, ignoreErrorMessage);

            Map<String, String> otherIgnoreErrorMap = new HashMap<>();
            otherIgnoreErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, otherIgnoreErrorClass);
            ignoreErrorsList.add(ignoreErrorMap);
            ignoreErrorsList.add(otherIgnoreErrorMap);

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreErrorsList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertTrue(config.getExpectedErrors().isEmpty());
            assertTrue(config.getExpectedStatusCodes().isEmpty());

            Set<IgnoreErrorConfig> ignoreErrors = config.getIgnoreErrors();
            assertFalse(ignoreErrors.isEmpty());
            assertEquals(2, ignoreErrors.size());
            logMocker.verifyNoMessage();

            for (IgnoreErrorConfig ignoreError : ignoreErrors) {
                assertNotNull(ignoreError);
                if (ignoreError.getErrorClass().equals(ignoreErrorClass)) {
                    assertEquals(ignoreErrorClass, ignoreError.getErrorClass());
                    assertEquals(ignoreError.getErrorMessage(), ignoreErrorMessage);
                    assertEquals(1, config.getIgnoreStatusCodes().size());
                    assertTrue(config.getIgnoreStatusCodes().contains(404));
                } else if (ignoreError.getErrorClass().equals(otherIgnoreErrorClass)) {
                    assertEquals(otherIgnoreErrorClass, ignoreError.getErrorClass());
                    assertNull(ignoreError.getErrorMessage());
                    assertEquals(1, config.getIgnoreStatusCodes().size());
                    assertTrue(config.getIgnoreStatusCodes().contains(404));
                } else {
                    fail("Unexpected error found");
                }
            }
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void ignoreErrorsStatusCodesDuplicates() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String ignoreStatusCodes = "502,502,,502";

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, ignoreStatusCodes);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertTrue(config.getExpectedStatusCodes().isEmpty());

            assertFalse(config.getIgnoreStatusCodes().isEmpty());
            assertEquals(1, config.getIgnoreStatusCodes().size());
            assertEquals(502, (int) Iterables.get(config.getIgnoreStatusCodes(), 0));
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void ignoreErrorsStatusCodesRange() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String ignoreStatusCodes = "500-509,";

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, ignoreStatusCodes);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertFalse(config.getIgnoreStatusCodes().isEmpty());
            assertEquals(10, config.getIgnoreStatusCodes().size());
            assertTrue(config.getIgnoreStatusCodes().contains(500));
            assertTrue(config.getIgnoreStatusCodes().contains(501));
            assertTrue(config.getIgnoreStatusCodes().contains(502));
            assertTrue(config.getIgnoreStatusCodes().contains(503));
            assertTrue(config.getIgnoreStatusCodes().contains(504));
            assertTrue(config.getIgnoreStatusCodes().contains(505));
            assertTrue(config.getIgnoreStatusCodes().contains(506));
            assertTrue(config.getIgnoreStatusCodes().contains(507));
            assertTrue(config.getIgnoreStatusCodes().contains(508));
            assertTrue(config.getIgnoreStatusCodes().contains(509));
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void ignoreErrorsStatusCodesInvalidDownwardRange() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String ignoreStatusCodes = "502-402";

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, ignoreStatusCodes);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertTrue(config.getIgnoreStatusCodes().isEmpty());
            logMocker.verifyMessage(Level.WARNING);
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void ignoreErrorsStatusCodesInvalidSizeRange() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String ignoreStatusCodes = "1-1001";

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, ignoreStatusCodes);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertTrue(config.getIgnoreStatusCodes().isEmpty());
            logMocker.verifyMessage(Level.WARNING);
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void ignoredErrorsStatusCodesRangeUnbounded() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String ignoreStatusCodes = "400-";

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, ignoreStatusCodes);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertTrue(config.getIgnoreStatusCodes().isEmpty());
            logMocker.verifyMessage(Level.WARNING);
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void ignoreErrorsStatusCodesInt() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            int ignoreStatusCode = 400;

            Map<String, Object> localSettings = createMap();

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, ignoreStatusCode);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertEquals(config.getIgnoreStatusCodes().size(), 1);
            assertTrue(config.getIgnoreStatusCodes().contains(ignoreStatusCode));
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void ignoreErrorsMessageDuplicate() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String ignoreErrorClass = "com.newrelic.IgnoreErrorClass";
            String ignoreErrorMessage = "dude";
            String duplicateErrorMessage = "edud";

            Map<String, Object> localSettings = createMap();

            Map<String, String> ignoreErrorMap = new HashMap<>();
            ignoreErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, ignoreErrorClass);
            ignoreErrorMap.put(ErrorCollectorConfigImpl.MESSAGE, ignoreErrorMessage);
            ignoreErrorMap.put(ErrorCollectorConfigImpl.MESSAGE, duplicateErrorMessage);

            List<Map<String, String>> ignoreErrorList = new ArrayList<>();
            ignoreErrorList.add(ignoreErrorMap);

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreErrorList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<IgnoreErrorConfig> ignoreErrors = config.getIgnoreErrors();
            assertFalse(ignoreErrors.isEmpty());
            IgnoreErrorConfig ignoreErrorConfig = Iterables.get(ignoreErrors, 0);
            assertNotNull(ignoreErrorConfig);
            assertEquals(ignoreErrorClass, ignoreErrorConfig.getErrorClass());
            assertEquals(duplicateErrorMessage, ignoreErrorConfig.getErrorMessage()); // last one wins (it's a map)
            assertEquals(1, config.getIgnoreStatusCodes().size());
            assertTrue(config.getIgnoreStatusCodes().contains(404));
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void ignoreErrorsClassMessageFallback() throws Exception {
        LogMocker logMocker = new LogMocker();

        try {
            String ignoreErrorClass = "com.newrelic.IgnoreErrorClass";
            String otherIgnoreErrorClass = "com.newrelic.OtherIgnoreErrorClass";
            String ignoreErrorMessage = "This error is ignored";

            Map<String, Object> localSettings = createMap();
            List<String> ignoreClassesList = new ArrayList<>();
            Map<String, List<String>> ignoreMessagesMap = new HashMap<>();

            // ignore_classes
            ignoreClassesList.add(otherIgnoreErrorClass);

            // ignore_messages
            List<String> ignoreMessagesList = new ArrayList<>();
            ignoreMessagesList.add(ignoreErrorMessage);
            ignoreMessagesMap.put(ignoreErrorClass, ignoreMessagesList);

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreClassesList);
            localSettings.put(ErrorCollectorConfigImpl.IGNORE_MESSAGES, ignoreMessagesMap);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertTrue(config.getExpectedErrors().isEmpty());
            assertTrue(config.getExpectedStatusCodes().isEmpty());

            Set<IgnoreErrorConfig> ignoreErrors = config.getIgnoreErrors();
            assertFalse(ignoreErrors.isEmpty());
            assertEquals(2, ignoreErrors.size());
            logMocker.verifyNoMessage();

            for (IgnoreErrorConfig ignoreError : ignoreErrors) {
                assertNotNull(ignoreError);
                if (ignoreError.getErrorClass().equals(ignoreErrorClass)) {
                    assertEquals(ignoreErrorClass, ignoreError.getErrorClass());
                    assertEquals(ignoreError.getErrorMessage(), ignoreErrorMessage);
                    assertEquals(1, config.getIgnoreStatusCodes().size());
                    assertTrue(config.getIgnoreStatusCodes().contains(404));
                } else if (ignoreError.getErrorClass().equals(otherIgnoreErrorClass)) {
                    assertEquals(otherIgnoreErrorClass, ignoreError.getErrorClass());
                    assertNull(ignoreError.getErrorMessage());
                    assertEquals(1, config.getIgnoreStatusCodes().size());
                    assertTrue(config.getIgnoreStatusCodes().contains(404));
                } else {
                    fail("Unexpected error found");
                }
            }
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void ignoreErrorsClassMessageFallbackDuplicate() throws Exception {
        LogMocker logMocker = new LogMocker();

        try {
            String ignoreErrorClass = "com.newrelic.IgnoreErrorClass";
            String otherIgnoreErrorClass = "com.newrelic.OtherIgnoreErrorClass";
            String ignoreErrorMessage = "This error is ignored";

            Map<String, Object> localSettings = createMap();
            List<String> ignoreClassesList = new ArrayList<>();
            Map<String, List<String>> ignoreMessagesMap = new HashMap<>();

            // ignore_classes
            ignoreClassesList.add(otherIgnoreErrorClass);
            ignoreClassesList.add(otherIgnoreErrorClass);

            // ignore_messages
            List<String> ignoreMessagesList = new ArrayList<>();
            ignoreMessagesList.add(ignoreErrorMessage);
            ignoreMessagesList.add(ignoreErrorMessage);
            ignoreMessagesMap.put(ignoreErrorClass, ignoreMessagesList);

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreClassesList);
            localSettings.put(ErrorCollectorConfigImpl.IGNORE_MESSAGES, ignoreMessagesMap);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            assertTrue(config.getExpectedErrors().isEmpty());
            assertTrue(config.getExpectedStatusCodes().isEmpty());

            Set<IgnoreErrorConfig> ignoreErrors = config.getIgnoreErrors();
            assertFalse(ignoreErrors.isEmpty());
            assertEquals(2, ignoreErrors.size());
            logMocker.verifyNoMessage();

            for (IgnoreErrorConfig ignoreError : ignoreErrors) {
                assertNotNull(ignoreError);
                if (ignoreError.getErrorClass().equals(ignoreErrorClass)) {
                    assertEquals(ignoreErrorClass, ignoreError.getErrorClass());
                    assertEquals(ignoreError.getErrorMessage(), ignoreErrorMessage);
                    assertEquals(1, config.getIgnoreStatusCodes().size());
                    assertTrue(config.getIgnoreStatusCodes().contains(404));
                } else if (ignoreError.getErrorClass().equals(otherIgnoreErrorClass)) {
                    assertEquals(otherIgnoreErrorClass, ignoreError.getErrorClass());
                    assertNull(ignoreError.getErrorMessage());
                    assertEquals(1, config.getIgnoreStatusCodes().size());
                    assertTrue(config.getIgnoreStatusCodes().contains(404));
                } else {
                    fail("Unexpected error found");
                }
            }
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void expectedErrorsClassMessageFallback() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String expectedErrorClass = "com.newrelic.ExpectedErrorClass";
            String otherExpectedErrorClass = "com.newrelic.OtherExpectedErrorClass";
            String expectedErrorMessage = "This error is expected";

            Map<String, Object> localSettings = createMap();
            List<String> expectedClassesList = new ArrayList<>();
            Map<String, List<String>> expectedMessagesMap = new HashMap<>();

            // expected_classes
            expectedClassesList.add(otherExpectedErrorClass);

            // expected_messages
            List<String> expectedMessagesList = new ArrayList<>();
            expectedMessagesList.add(expectedErrorMessage);
            expectedMessagesMap.put(expectedErrorClass, expectedMessagesList);

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_CLASSES, expectedClassesList);
            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_MESSAGES, expectedMessagesMap);

            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<ExpectedErrorConfig> expectedErrors = config.getExpectedErrors();
            assertFalse(expectedErrors.isEmpty());
            assertEquals(2, expectedErrors.size());

            ExpectedErrorConfig expectedError = Iterables.get(expectedErrors, 1);

            assertNotNull(expectedError);
            assertEquals(expectedErrorClass, expectedError.getErrorClass());
            assertEquals(expectedError.getErrorMessage(), expectedErrorMessage);

            assertTrue(config.getExpectedStatusCodes().isEmpty());
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    @Test
    public void expectedErrorsClassMessageFallbackDuplicate() throws Exception {
        LogMocker logMocker = new LogMocker();
        try {
            String expectedErrorClass = "com.newrelic.ExpectedErrorClass";
            String otherExpectedErrorClass = "com.newrelic.OtherExpectedErrorClass";
            String expectedErrorMessage = "This error is expected";

            Map<String, Object> localSettings = createMap();
            List<String> expectedClassesList = new ArrayList<>();
            Map<String, List<String>> expectedMessagesMap = new HashMap<>();

            // expected_classes
            expectedClassesList.add(otherExpectedErrorClass);
            expectedClassesList.add(otherExpectedErrorClass);

            // expected_messages
            List<String> expectedMessagesList = new ArrayList<>();
            expectedMessagesList.add(expectedErrorMessage);
            expectedMessagesList.add(expectedErrorMessage);
            expectedMessagesMap.put(expectedErrorClass, expectedMessagesList);

            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_CLASSES, expectedClassesList);
            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_MESSAGES, expectedMessagesMap);

            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<ExpectedErrorConfig> expectedErrors = config.getExpectedErrors();
            assertFalse(expectedErrors.isEmpty());
            assertEquals(2, expectedErrors.size());

            ExpectedErrorConfig expectedError = Iterables.get(expectedErrors, 1);

            assertNotNull(expectedError);
            assertEquals(expectedErrorClass, expectedError.getErrorClass());
            assertEquals(expectedError.getErrorMessage(), expectedErrorMessage);

            assertTrue(config.getExpectedStatusCodes().isEmpty());
            logMocker.verifyNoMessage();
        } finally {
            logMocker.close();
        }
    }

    private void verifyIgnoredErrorClassAndMessage(ErrorCollectorConfig config, String ignoredClassName, String ignoredMessage) {
        Set<IgnoreErrorConfig> ignoreErrors = config.getIgnoreErrors();
        assertNotNull(ignoreErrors);
        boolean errorClassAndMessageFound = false;
        for (IgnoreErrorConfig ignoreError : ignoreErrors) {
            if (ignoreError.getErrorClass().equals(ignoredClassName)) {
                errorClassAndMessageFound = true;
                if (ignoredMessage != null) {
                    errorClassAndMessageFound = ignoreError.getErrorMessage().equals(ignoredMessage);
                }
            }
        }
        
        if (!errorClassAndMessageFound) {
            fail("Ignored error class: " + ignoredClassName + " and message: \"" + ignoredMessage + "\" not found.");
        }
    }

    @Test // JAVA-2974
    public void logNoWarningWhenExpectedErrorsEmpty() {
        LogMocker mocker = new LogMocker();
        try {
            Map<String, Object> localSettings = createMap();
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);
            mocker.verifyNoMessage();
            Assert.assertTrue(config.isEnabled());
        } finally {
            mocker.close();
        }
    }

    @Test
    public void logWarningWhenExpectedErrorsInvalidType() {
        LogMocker mocker = new LogMocker();
        try {
            Boolean nonNumericValue = false;
            Map<String, Object> localSettings = createMap();
            localSettings.put(ErrorCollectorConfigImpl.EXPECTED_STATUS_CODES, nonNumericValue);

            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);
            // Don't "over-verify" on message details - just assert that we logged some warning.
            mocker.verifyMessage(Level.WARNING);
            Assert.assertTrue(config.isEnabled());
        } finally {
            mocker.close();
        }
    }

    @Test
    public void ignoreErrorsClassWithLeadingSpaces() {
        LogMocker mocker = new LogMocker();
        try {
            String ignoreErrorClass = "com.newrelic.IgnoreErrorClass";

            Map<String, Object> localSettings = createMap();
            List<String> ignoreClassesList = new ArrayList<>();
            ignoreClassesList.add("  " + ignoreErrorClass);

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreClassesList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<IgnoreErrorConfig> ignoreErrors = config.getIgnoreErrors();
            assertFalse(ignoreErrors.isEmpty());
            assertEquals(1, ignoreErrors.size());

            IgnoreErrorConfig ignoreError = ignoreErrors.iterator().next();
            assertEquals(ignoreErrorClass, ignoreError.getErrorClass());
            assertNull(ignoreError.getErrorMessage());

            mocker.verifyNoMessage();
        } finally {
            mocker.close();
        }
    }

    @Test
    public void ignoreErrorsClassWithTrailingSpaces() {
        LogMocker mocker = new LogMocker();
        try {
            String ignoreErrorClass = "com.newrelic.IgnoreErrorClass";

            Map<String, Object> localSettings = createMap();
            List<String> ignoreClassesList = new ArrayList<>();
            ignoreClassesList.add(ignoreErrorClass + "  ");

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreClassesList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<IgnoreErrorConfig> ignoreErrors = config.getIgnoreErrors();
            assertFalse(ignoreErrors.isEmpty());
            assertEquals(1, ignoreErrors.size());

            IgnoreErrorConfig ignoreError = ignoreErrors.iterator().next();
            assertEquals(ignoreErrorClass, ignoreError.getErrorClass());
            assertNull(ignoreError.getErrorMessage());

            mocker.verifyNoMessage();
        } finally {
            mocker.close();
        }
    }

    @Test
    public void ignoreErrorsClassWithLeadingAndTrailingSpaces() {
        LogMocker mocker = new LogMocker();
        try {
            String ignoreErrorClass = "com.newrelic.IgnoreErrorClass";

            Map<String, Object> localSettings = createMap();
            List<String> ignoreClassesList = new ArrayList<>();
            ignoreClassesList.add(" " + ignoreErrorClass + " ");

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreClassesList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<IgnoreErrorConfig> ignoreErrors = config.getIgnoreErrors();
            assertFalse(ignoreErrors.isEmpty());
            assertEquals(1, ignoreErrors.size());

            IgnoreErrorConfig ignoreError = ignoreErrors.iterator().next();
            assertEquals(ignoreErrorClass, ignoreError.getErrorClass());
            assertNull(ignoreError.getErrorMessage());

            mocker.verifyNoMessage();
        } finally {
            mocker.close();
        }
    }

    @Test
    public void ignoreErrorsClassWithNewlinePrefix() {
        LogMocker mocker = new LogMocker();
        try {
            String ignoreErrorClass = "com.newrelic.IgnoreErrorClass";

            Map<String, Object> localSettings = createMap();
            List<String> ignoreClassesList = new ArrayList<>();
            ignoreClassesList.add("\n" + ignoreErrorClass);

            localSettings.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreClassesList);
            ErrorCollectorConfig config = ErrorCollectorConfigImpl.createErrorCollectorConfig(localSettings);

            Set<IgnoreErrorConfig> ignoreErrors = config.getIgnoreErrors();
            assertFalse(ignoreErrors.isEmpty());
            assertEquals(1, ignoreErrors.size());

            IgnoreErrorConfig ignoreError = ignoreErrors.iterator().next();
            assertEquals(ignoreErrorClass, ignoreError.getErrorClass());
            assertNull(ignoreError.getErrorMessage());

            mocker.verifyNoMessage();
        } finally {
            mocker.close();
        }
    }

    // Mock out the Logger so we can verify warnings were (or were not) logged at a given level.
    // Note: do not "over verify" by checking the log message itself - too brittle.
    private class LogMocker {
        private final Agent savedAgent;
        private final Agent mockAgent;
        private final Logger mockLogger;
        private final MetricAggregator mockMetricAggregator;

        public LogMocker() {
            savedAgent = AgentBridge.getAgent();
            mockAgent = Mockito.mock(Agent.class);
            mockLogger = Mockito.mock(Logger.class);
            mockMetricAggregator = Mockito.mock(MetricAggregator.class);
            Mockito.when(mockAgent.getLogger()).thenReturn(mockLogger);
            Mockito.when(mockAgent.getMetricAggregator()).thenReturn(mockMetricAggregator);
            AgentBridge.agent = mockAgent;
        }

        public void verifyNoMessage() {
            Mockito.verify(mockLogger, Mockito.never()).log(
                    Mockito.any(Level.class), Mockito.anyString(), Mockito.any());
        }

        public void verifyMessage(Level at) {
            Mockito.verify(mockLogger).log(Mockito.eq(at), Mockito.anyString(), Mockito.any(),
                    Mockito.any(), Mockito.any());
        }

        public void close() {
            AgentBridge.agent = savedAgent;
        }
    }
}
