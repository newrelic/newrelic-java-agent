/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.SpanCategory;
import com.newrelic.agent.model.SpanError;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.HttpParameters;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static com.newrelic.agent.service.analytics.SpanEventFactory.DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
    }

    @Test
    public void addressShouldBeSet() {
        SpanEvent target = spanEventFactory.setAddress("localhost", "3306").build();

        assertEquals("localhost:3306", target.getIntrinsics().get("peer.address"));
    }

    @Test
    public void shouldTruncate3KDBStatementTo2K() {
        char[] data = new char[3000];
        String threeKStatement = new String(data);

        SpanEvent target = spanEventFactory.setDatabaseStatement(threeKStatement).build();

        assertEquals(2000,
                target.getIntrinsics().get("db.statement").toString().length());
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
        SpanEvent spanEvent = spanEventFactory.setHttpStatusCode(418).build();

        assertEquals(418, spanEvent.getAgentAttributes().get("http.statusCode"));
    }

    @Test
    public void shouldNotSetStatusCodeWhenFiltering() {
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
        SpanEvent spanEvent = spanEventFactory.setHttpStatusText("I'm a teapot.").build();

        assertEquals("I'm a teapot.", spanEvent.getAgentAttributes().get("http.statusText"));
    }

    @Test
    public void shouldNotSetStatusTextWhenFiltering() {
        SpanEventFactory factory = new SpanEventFactory("blerb", new PassNothingAttributeFilter(), DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);
        SpanEvent spanEvent = factory.setHttpStatusText("I'm a teapot.").build();

        assertNull(spanEvent.getAgentAttributes().get("http.statusText"));
    }

    @Test
    public void shouldNotSetNullStatusText() {
        SpanEvent spanEvent = spanEventFactory.setHttpStatusText(null).build();

        assertFalse(spanEvent.getAgentAttributes().containsKey("http.statusText"));
    }

    @Test
    public void shouldSetDataStoreParameters() {
        DatastoreParameters mockParameters = mock(DatastoreParameters.class);
        when(mockParameters.getDatabaseName()).thenReturn("database name");

        SpanEvent target = spanEventFactory.setExternalParameterAttributes(mockParameters).build();

        assertEquals("database name", target.getIntrinsics().get("db.instance"));
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
}




