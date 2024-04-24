/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.SpanCategory;
import com.newrelic.agent.model.SpanError;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.HttpParameters;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.newrelic.agent.service.analytics.SpanEventFactory.DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpanEventFactoryTest {

    SpanEventFactory spanEventFactory = new SpanEventFactory("green", new AttributeFilter.PassEverythingAttributeFilter(), DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);

    @Test
    public void appNameShouldBeSet() {
        SpanEvent target = spanEventFactory.build();

        assertEquals("green", target.getAppName());

    }

    @Test
    public void categoryShouldBeSet() {
        SpanEvent target = spanEventFactory.setCategory(SpanCategory.http).build();

        assertEquals(SpanCategory.http, target.getCategory());
    }

    @Test
    public void uriShouldBeSet() {
        SpanEvent target = spanEventFactory.setUri(URI.create("https://newrelic.com")).build();

        assertEquals("https://newrelic.com", target.getAgentAttributes().get("http.url"));
        assertEquals("newrelic.com", target.getAgentAttributes().get("server.address"));
        assertEquals("newrelic.com", target.getAgentAttributes().get("peer.hostname"));
        assertNull(target.getAgentAttributes().get("server.port"));
    }

    @Test
    public void addressShouldBeSet() {
        SpanEvent target = spanEventFactory.setServerAddress("localhost").setServerPort(3306).build();

        assertEquals("localhost", target.getAgentAttributes().get("server.address"));
        assertEquals("localhost", target.getAgentAttributes().get("peer.hostname"));
        assertEquals(3306, target.getAgentAttributes().get("server.port"));
    }

    @Test
    public void shouldTruncate5KDBStatementTo4K() {
        char[] data = new char[5000];
        String fiveKStatement = new String(data);

        SpanEvent target = spanEventFactory.setDatabaseStatement(fiveKStatement).build();

        assertEquals(4095,
                target.getAgentAttributes().get("db.statement").toString().length());
    }

    @Test
    public void shouldSetSpanErrorWithOnlyErrorClassAndMessage() {
        SpanError spanError = new SpanError();
        spanError.setErrorClass(RuntimeException.class);
        spanError.setErrorStatus(500);
        spanError.setErrorMessage("not again");

        SpanEvent target = spanEventFactory.setSpanError(spanError).build();

        assertEquals(RuntimeException.class.getName(), target.getAgentAttributes().get("error.class"));
        assertEquals(null, target.getAgentAttributes().get("error.status"));
        assertEquals("not again", target.getAgentAttributes().get("error.message"));
    }

    @Test
    public void shouldNotSetSpanErrorWhenFiltered() {
        SpanError spanError = new SpanError();
        spanError.setErrorClass(RuntimeException.class);
        spanError.setErrorStatus(500);
        spanError.setErrorMessage("not again");

        SpanEventFactory target = new SpanEventFactory("blerb", new PassNothingAttributeFilter(), DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);
        SpanEvent spanEvent = target.setSpanError(spanError).build();

        assertEquals(0, spanEvent.getAgentAttributes().size());
    }

    @Test
    public void shouldSetHttpParameters() {
        mockAttributeConfig(HttpAttrMode.BOTH);
        HttpParameters mockParameters = mock(HttpParameters.class);
        when(mockParameters.getLibrary()).thenReturn("library");
        when(mockParameters.getProcedure()).thenReturn("procedure");

        SpanEvent target = spanEventFactory.setExternalParameterAttributes(mockParameters).build();

        assertEquals("library", target.getIntrinsics().get("component"));
        assertEquals("procedure", target.getAgentAttributes().get("http.method"));
    }

    @Test
    public void doesNotSetHttpAgentAttributesWhenFiltering() {
        HttpParameters mockParameters = mock(HttpParameters.class);
        when(mockParameters.getLibrary()).thenReturn("library");
        when(mockParameters.getProcedure()).thenReturn("procedure");

        SpanEventFactory target = new SpanEventFactory("blerb", new PassNothingAttributeFilter(), DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);
        SpanEvent spanEvent = target.setExternalParameterAttributes(mockParameters).build();

        assertEquals("library", spanEvent.getIntrinsics().get("component"));
        assertNull(spanEvent.getAgentAttributes().get("http.method"));
    }

    @Test
    public void shouldSetStatusCode() {
        mockAttributeConfig(HttpAttrMode.BOTH);
        SpanEvent spanEvent = spanEventFactory.setHttpStatusCode(418).build();

        assertEquals(418, spanEvent.getAgentAttributes().get("http.statusCode"));
        assertEquals(418, spanEvent.getAgentAttributes().get("httpResponseCode"));
    }

    @Test
    public void shouldSetStandardStatusCode() {
        mockAttributeConfig(HttpAttrMode.STANDARD);
        SpanEvent spanEvent = spanEventFactory.setHttpStatusCode(418).build();

        assertEquals(418, spanEvent.getAgentAttributes().get("http.statusCode"));
        assertNull(spanEvent.getAgentAttributes().get("httpResponseCode"));
    }

    @Test
    public void shouldSetLegacyStatusCode() {
        mockAttributeConfig(HttpAttrMode.LEGACY);
        SpanEvent spanEvent = spanEventFactory.setHttpStatusCode(418).build();

        assertNull(spanEvent.getAgentAttributes().get("http.statusCode"));
        assertEquals(418, spanEvent.getAgentAttributes().get("httpResponseCode"));
    }

    @Test
    public void shouldNotSetStatusCodeWhenFiltering() {
        mockAttributeConfig(HttpAttrMode.BOTH);
        SpanEventFactory factory = new SpanEventFactory("blerb", new PassNothingAttributeFilter(), DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);
        SpanEvent spanEvent = factory.setHttpStatusCode(418).build();

        assertNull(spanEvent.getAgentAttributes().get("http.statusCode"));
    }

    @Test
    public void shouldNotSetNullStatusCode() {
        SpanEvent spanEvent = spanEventFactory.setHttpStatusCode(null).build();

        assertFalse(spanEvent.getAgentAttributes().containsKey("http.statusCode"));
    }

    @Test
    public void shouldSetStatusText() {
        mockAttributeConfig(HttpAttrMode.BOTH);
        SpanEvent spanEvent = spanEventFactory.setHttpStatusText("I'm a teapot.").build();

        assertEquals("I'm a teapot.", spanEvent.getAgentAttributes().get("http.statusText"));
        assertEquals("I'm a teapot.", spanEvent.getAgentAttributes().get("httpResponseMessage"));
    }

    @Test
    public void shouldSetStandardStatusText() {
        mockAttributeConfig(HttpAttrMode.STANDARD);
        SpanEvent spanEvent = spanEventFactory.setHttpStatusText("I'm a teapot.").build();

        assertEquals("I'm a teapot.", spanEvent.getAgentAttributes().get("http.statusText"));
        assertNull(spanEvent.getAgentAttributes().get("httpResponseMessage"));
    }

    @Test
    public void shouldSetLegacyStatusText() {
        mockAttributeConfig(HttpAttrMode.LEGACY);
        SpanEvent spanEvent = spanEventFactory.setHttpStatusText("I'm a teapot.").build();

        assertNull(spanEvent.getAgentAttributes().get("http.statusText"));
        assertEquals("I'm a teapot.", spanEvent.getAgentAttributes().get("httpResponseMessage"));
    }

    @Test
    public void shouldNotSetStatusTextWhenFiltering() {
        mockAttributeConfig(HttpAttrMode.BOTH);
        SpanEventFactory factory = new SpanEventFactory("blerb", new PassNothingAttributeFilter(), DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);
        SpanEvent spanEvent = factory.setHttpStatusText("I'm a teapot.").build();

        assertNull(spanEvent.getAgentAttributes().get("http.statusText"));
        assertNull(spanEvent.getAgentAttributes().get("httpResponseMessage"));
    }

    @Test
    public void shouldNotSetNullStatusText() {
        SpanEvent spanEvent = spanEventFactory.setHttpStatusText(null).build();

        assertFalse(spanEvent.getAgentAttributes().containsKey("http.statusText"));
        assertFalse(spanEvent.getAgentAttributes().containsKey("httpResponseMessage"));
    }

    @Test
    public void shouldSetDataStoreParameters() {
        DatastoreParameters mockParameters = mock(DatastoreParameters.class);
        when(mockParameters.getDatabaseName()).thenReturn("database name");
        when(mockParameters.getOperation()).thenReturn("select");
        when(mockParameters.getCollection()).thenReturn("users");
        when(mockParameters.getProduct()).thenReturn("MySQL");
        when(mockParameters.getHost()).thenReturn("dbserver");
        when(mockParameters.getPort()).thenReturn(3306);

        SpanEvent target = spanEventFactory.setExternalParameterAttributes(mockParameters).build();

        assertEquals("database name", target.getAgentAttributes().get("db.instance"));
        assertEquals("select", target.getAgentAttributes().get("db.operation"));
        assertEquals("users", target.getAgentAttributes().get("db.collection"));
        assertEquals("MySQL", target.getAgentAttributes().get("db.system"));
        assertEquals("dbserver", target.getAgentAttributes().get("peer.hostname"));
        assertEquals("dbserver", target.getAgentAttributes().get("server.address"));
        assertEquals(3306, target.getAgentAttributes().get("server.port"));
        assertEquals("dbserver:3306", target.getAgentAttributes().get("peer.address"));
    }

    @Test
    public void shouldStoreStackTrace() {
        SpanEventFactory spanEventFactory = new SpanEventFactory("MyApp", new AttributeFilter.PassEverythingAttributeFilter(),
                DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);
        spanEventFactory.setKindFromUserAttributes();
        MockServiceManager serviceManager = new MockServiceManager();
        serviceManager.setConfigService(new MockConfigService(mock(AgentConfig.class)));
        ServiceFactory.setServiceManager(serviceManager);
        spanEventFactory.setStackTraceAttributes(
                ImmutableMap.of(DefaultTracer.BACKTRACE_PARAMETER_NAME, Arrays.asList(Thread.currentThread().getStackTrace())));

        final Object stackTrace = spanEventFactory.build().getAgentAttributes().get(AttributeNames.CODE_STACKTRACE);
        assertNotNull(stackTrace);
    }

    @Test
    public void shouldSetCLMParameters() {
        Map<String, Object> agentAttributes = ImmutableMap.of(
                AttributeNames.CLM_NAMESPACE, "nr",
                AttributeNames.CLM_FUNCTION, "process",
                AttributeNames.THREAD_ID, 666
        );

        SpanEvent target = spanEventFactory.setClmAttributes(agentAttributes).build();

        assertEquals("nr", target.getAgentAttributes().get(AttributeNames.CLM_NAMESPACE));
        assertEquals("process", target.getAgentAttributes().get(AttributeNames.CLM_FUNCTION));
        assertEquals(666, target.getIntrinsics().get(AttributeNames.THREAD_ID));
    }

    @Test
    public void shouldFilterUserAttributes() {
        SpanEventFactory target = new SpanEventFactory("blerb", new AttributeFilter.PassEverythingAttributeFilter() {
            @Override
            public Map<String, ?> filterUserAttributes(String appName, Map<String, ?> userAttributes) {
                return Collections.<String, Object>singletonMap("filtered", "yes");
            }
        }, DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);

        SpanEvent spanEvent = target.setUserAttributes(Collections.<String, Object>singletonMap("original", "sad")).build();

        assertEquals("yes", spanEvent.getUserAttributesCopy().get("filtered"));
        assertNull(spanEvent.getUserAttributesCopy().get("original"));
    }

    @Test
    public void shouldFilterAgentAttributes() {
        SpanEventFactory target = new SpanEventFactory("blerb", new AttributeFilter.PassEverythingAttributeFilter() {
            @Override
            public Map<String, ?> filterAgentAttributes(String appName, Map<String, ?> agentAttributes) {
                return Collections.<String, Object>singletonMap("filtered", "yes");
            }
        }, DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);

        SpanEvent spanEvent = target.putAllAgentAttributes(Collections.<String, Object>singletonMap("original", "sad")).build();

        assertEquals("yes", spanEvent.getAgentAttributes().get("filtered"));
        assertNull(spanEvent.getAgentAttributes().get("original"));
    }

    private static class PassNothingAttributeFilter extends AttributeFilter.PassEverythingAttributeFilter {
        @Override
        public boolean shouldIncludeAgentAttribute(String appName, String attributeName) {
            return false;
        }

        @Override
        public Map<String, ?> filterAgentAttributes(String appName, Map<String, ?> agentAttributes) {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, ?> filterUserAttributes(String appName, Map<String, ?> userAttributes) {
            return Collections.emptyMap();
        }
    }

    /**
     * These should never be both false.
     */
    private void mockAttributeConfig(HttpAttrMode httpAttrMode) {
        MockServiceManager serviceManager = new MockServiceManager();
        AgentConfig agentConfig = mock(AgentConfig.class, RETURNS_DEEP_STUBS);
        serviceManager.setConfigService(new MockConfigService(agentConfig));

        when(agentConfig.getAttributesConfig().isStandardHttpAttr())
                .thenReturn(httpAttrMode != HttpAttrMode.LEGACY);

        when(agentConfig.getAttributesConfig().isLegacyHttpAttr())
                .thenReturn(httpAttrMode != HttpAttrMode.STANDARD);
    }

    private enum HttpAttrMode {
        BOTH,
        STANDARD,
        LEGACY,
    }
}
