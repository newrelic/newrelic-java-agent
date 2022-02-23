/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.*;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.ErrorEventFactory;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.newrelic.agent.config.ConfigConstant.*;
import static com.newrelic.agent.config.ConfigHelper.buildConfigMap;
import static org.junit.Assert.assertEquals;

public class ExpectedErrorFactoryTest {
    private static String appName;

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
        appName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ServiceManager serviceManager = ServiceFactory.getServiceManager();
        if (serviceManager != null) {
            serviceManager.stop();
        }
    }

    @Test
    public void httpExpectedError() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("error_collector:expected_status_codes", 403);
        configuration = buildConfigMap(configuration);

        ErrorCollectorConfig errorCollectorConfig = setupServices(configuration);

        TracedError error = HttpTracedError.builder(errorCollectorConfig, appName, "dude",
                System.currentTimeMillis()).statusCodeAndMessage(403, null).requestUri("/dude").build();

        JSONObject intrinsic = getIntrinsics(error);
        Assert.assertTrue((Boolean) intrinsic.get("error.expected"));
    }

    @Test
    public void httpNonExpectedError() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("error_collector:expected_status_codes", 420);
        configuration = buildConfigMap(configuration);

        ErrorCollectorConfig errorCollectorConfig = setupServices(configuration);

        TracedError error = HttpTracedError.builder(errorCollectorConfig, appName, "dude",
                System.currentTimeMillis()).statusCodeAndMessage(403, null).requestUri("/dude").build();

        JSONObject intrinsic = getIntrinsics(error);
        Assert.assertFalse((Boolean) intrinsic.get("error.expected"));
    }

    @Test
    public void httpExpectedErrorRange() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("error_collector:expected_status_codes", "403-666");
        configuration = buildConfigMap(configuration);

        ErrorCollectorConfig errorCollectorConfig = setupServices(configuration);

        TracedError error = HttpTracedError.builder(errorCollectorConfig, appName, "dude",
                System.currentTimeMillis()).statusCodeAndMessage(403, null).requestUri("/dude").build();

        JSONObject intrinsic = getIntrinsics(error);
        Assert.assertTrue((Boolean) intrinsic.get("error.expected"));
    }

    @Test
    public void httpNonExpectedErrorRange() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("error_collector:expected_status_codes", "421-666");
        configuration = buildConfigMap(configuration);

        ErrorCollectorConfig errorCollectorConfig = setupServices(configuration);

        TracedError error = HttpTracedError.builder(errorCollectorConfig, appName, "dude",
                System.currentTimeMillis()).statusCodeAndMessage(420, null).requestUri("/dude").build();

        JSONObject intrinsic = getIntrinsics(error);
        Assert.assertFalse((Boolean) intrinsic.get("error.expected"));
    }

    @Test
    public void throwableExpectedError() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        Map<String, Object> errorClasses = new HashMap<>();
        ArrayList<Map> list = new ArrayList<>();
        list.add(errorClasses);

        errorClasses.put("class_name", "java.lang.UnsupportedClassVersionError");
        configuration.put("error_collector:expected_classes", list);
        configuration = buildConfigMap(configuration);

        setupServices(configuration);

        String msg = "Oh dude!";
        Error e = new UnsupportedClassVersionError(msg);
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getErrorCollectorConfig();
        TracedError error = ThrowableError.builder(errorCollectorConfig, null, null, e, System.currentTimeMillis()).build();

        JSONObject intrinsic = getIntrinsics(error);
        Assert.assertTrue((Boolean) intrinsic.get("error.expected"));
    }

    @Test
    public void throwableNonExpectedError() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        Map<String, Object> errorClasses = new HashMap<>();
        ArrayList<Map> list = new ArrayList<>();
        list.add(errorClasses);

        errorClasses.put("class_name", "java.lang.UnsupportedClasksVersionError");
        configuration.put("error_collector:expected_classes", list);
        configuration = buildConfigMap(configuration);

        setupServices(configuration);

        String msg = "Oh dude!";
        Error e = new UnsupportedClassVersionError(msg);
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getErrorCollectorConfig();
        TracedError error = ThrowableError.builder(errorCollectorConfig, null, null, e, System.currentTimeMillis()).build();

        JSONObject intrinsic = getIntrinsics(error);
        Assert.assertFalse((Boolean) intrinsic.get("error.expected"));
    }

    @Test
    public void throwableExpectedErrorAndMessage() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        Map<String, Object> errorClasses = new HashMap<>();
        ArrayList<Map> list = new ArrayList<>();
        list.add(errorClasses);

        errorClasses.put("class_name", "java.lang.UnsupportedClassVersionError");
        errorClasses.put("message", "my old friend");
        configuration.put("error_collector:expected_classes", list);
        configuration = buildConfigMap(configuration);

        setupServices(configuration);

        String msg = "my old friend";
        Error e = new UnsupportedClassVersionError(msg);
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getErrorCollectorConfig();
        TracedError error = ThrowableError.builder(errorCollectorConfig, null, null, e, System.currentTimeMillis()).build();

        JSONObject intrinsic = getIntrinsics(error);
        Assert.assertTrue((Boolean) intrinsic.get("error.expected"));
    }

    @Test
    public void throwableExpectedErrorAndMessageShort() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        Map<String, Object> errorClasses = new HashMap<>();
        ArrayList<Map> list = new ArrayList<>();
        list.add(errorClasses);

        errorClasses.put("class_name", "java.lang.UnsupportedClassVersionError");
        errorClasses.put("message", "my old friend");
        configuration.put("error_collector:expected_classes", list);
        configuration = buildConfigMap(configuration);

        setupServices(configuration);

        String msg = "my old friend. I've come to talk with you again";
        Error e = new UnsupportedClassVersionError(msg);
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getErrorCollectorConfig();
        TracedError error = ThrowableError.builder(errorCollectorConfig, null, null, e, System.currentTimeMillis()).build();

        JSONObject intrinsic = getIntrinsics(error);
        Assert.assertTrue((Boolean) intrinsic.get("error.expected"));
    }

    @Test
    public void throwableExpectedErrorAndWrongMessage() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        Map<String, Object> errorClasses = new HashMap<>();
        ArrayList<Map> list = new ArrayList<>();
        list.add(errorClasses);

        errorClasses.put("class_name", "java.lang.UnsupportedClassVersionError");
        errorClasses.put("message", "my old friend");
        configuration.put("error_collector:expected_classes", list);
        configuration = buildConfigMap(configuration);

        setupServices(configuration);

        String msg = "my young friend";
        Error e = new UnsupportedClassVersionError(msg);
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getErrorCollectorConfig();
        TracedError error = ThrowableError.builder(errorCollectorConfig, null, null, e, System.currentTimeMillis()).build();

        JSONObject intrinsic = getIntrinsics(error);
        Assert.assertNotNull(intrinsic);
        Assert.assertFalse((Boolean) intrinsic.get("error.expected"));
    }

    private ErrorCollectorConfig setupServices(Map<String, Object> configuration) {
        configuration.put(AgentConfigImpl.APP_NAME, "Unit Test");
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(configuration);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig,
                Collections.<String, Object>emptyMap());

        AttributesService attService = new AttributesService();
        MockRPMServiceManager rpmService = new MockRPMServiceManager();

        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.setAttributesService(attService);
        serviceManager.setRPMServiceManager(rpmService);
        return serviceManager.getConfigService().getDefaultAgentConfig().getErrorCollectorConfig();
    }

    private JSONObject getIntrinsics(TracedError tracedError) throws Exception {
        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(tracedError);
        Assert.assertNotNull(serializedError);
        JSONObject params = (JSONObject) serializedError.get(4);
        return (JSONObject) params.get("intrinsics");
    }

    @Test
    public void testExpectedErrorEvent() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("error_collector:expected_status_codes", 403);
        configuration = buildConfigMap(configuration);

        ErrorCollectorConfig errorCollectorConfig = setupServices(configuration);

        TracedError error = HttpTracedError.builder(errorCollectorConfig, appName, "dude",
                System.currentTimeMillis()).statusCodeAndMessage(403, null).requestUri("/dude").build();

        ErrorEvent event = ErrorEventFactory.create(appName, error, DistributedTraceServiceImpl.nextTruncatedFloat());

        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(true, jsonObject.get("error.expected"));
    }

    @Test
    public void testNonExpectedErrorEvent() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("error_collector:expected_status_codes", 403);
        configuration = buildConfigMap(configuration);

        ErrorCollectorConfig errorCollectorConfig = setupServices(configuration);

        TracedError error = HttpTracedError.builder(errorCollectorConfig, appName, "dude",
                System.currentTimeMillis()).statusCodeAndMessage(420, null).requestUri("/dude").build();

        ErrorEvent event = ErrorEventFactory.create(appName, error, DistributedTraceServiceImpl.nextTruncatedFloat());

        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        Assert.assertFalse((Boolean) jsonObject.get("error.expected"));
    }

    @Test
    public void testNonTruncatedErrorEvent() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        configuration = buildConfigMap(configuration);
        ErrorCollectorConfig errorCollectorConfig = setupServices(configuration);

        StringBuilder errorMessageBuilder = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            errorMessageBuilder.append("1");
        }
        String errorMessage = errorMessageBuilder.toString();

        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, appName, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, errorMessage)
                .requestUri("/dude")
                .build();

        ErrorEvent event = ErrorEventFactory.create(appName, error, DistributedTraceServiceImpl.nextTruncatedFloat());

        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(errorMessage, error.getMessage());
        assertEquals(errorMessage, error.getExceptionClass());
        assertEquals(errorMessage, event.getErrorClass());
        assertEquals(errorMessage, jsonObject.get("error.class"));
        assertEquals(errorMessage, jsonObject.get("error.message"));
        assertEquals(250, jsonObject.get("error.message").toString().length());
        assertEquals(250, event.getErrorClass().length());
        assertEquals(250, event.getErrorMessage().length());
    }

    @Test
    public void testTruncatedErrorEvent() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        configuration = buildConfigMap(configuration);
        ErrorCollectorConfig errorCollectorConfig = setupServices(configuration);

        StringBuilder errorMessageBuilder = new StringBuilder();
        for (int i = 0; i < 1024; i++) {
            errorMessageBuilder.append("1");
        }
        String errorMessage = errorMessageBuilder.toString();

        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, appName, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, errorMessage)
                .requestUri("/dude")
                .build();

        ErrorEvent event = ErrorEventFactory.create(appName, error, DistributedTraceServiceImpl.nextTruncatedFloat());

        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(errorMessage, error.getMessage());
        assertEquals("HttpClientError 403", error.getExceptionClass());
        assertEquals("HttpClientError 403", event.getErrorClass());
        assertEquals("HttpClientError 403", jsonObject.get("error.class"));
        assertEquals(MAX_ERROR_MESSAGE_SIZE, jsonObject.get("error.message").toString().length());
        assertEquals(MAX_ERROR_MESSAGE_SIZE, event.getErrorMessage().length());
    }
}
