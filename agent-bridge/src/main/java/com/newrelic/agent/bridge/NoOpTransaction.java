/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class NoOpTransaction implements Transaction {
    public static final Transaction INSTANCE = new NoOpTransaction();
    public static final NoOpMap<String, Object> AGENT_ATTRIBUTES = new NoOpMap<>();
    public static final NoOpMap<String, Object> USER_ATTRIBUTES = new NoOpMap<>();

    @Override
    public void beforeSendResponseHeaders() {
    }

    @Override
    public void addOutboundResponseHeaders() {
    }

    @Override
    public boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category,
            String... parts) {
        return false;
    }

    @Override
    public boolean setTransactionName(com.newrelic.agent.bridge.TransactionNamePriority namePriority, boolean override,
            String category, String... parts) {
        return false;
    }

    @Override
    public boolean isTransactionNameSet() {
        return false;
    }

    @Override
    public String getTransactionName() {
        return "";
    }

    @Override
    public TracedMethod getLastTracer() {
        return NoOpTracedMethod.INSTANCE;
    }

    @Override
    public TracedMethod getTracedMethod() {
        return NoOpTracedMethod.INSTANCE;
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public void setApplicationName(ApplicationNamePriority priority, String appName) {
    }

    @Override
    public boolean isAutoAppNamingEnabled() {
        return false;
    }

    @Override
    public boolean isWebRequestSet() {
        return false;
    }

    @Override
    public boolean isWebResponseSet() {
        return false;
    }

    @Override
    public void setWebRequest(Request request) {
    }

    @Override
    public void setWebResponse(Response response) {
    }

    @Override
    public WebResponse getWebResponse() {
        return NoOpWebResponse.INSTANCE;
    }

    @Override
    public void convertToWebTransaction() {
    }

    @Override
    public boolean isWebTransaction() {
        return false;
    }

    @Override
    public void requestInitialized(Request request, Response response) {
    }

    @Override
    public void requestDestroyed() {
    }

    @Override
    public void ignore() {
    }

    @Override
    public void ignoreApdex() {
    }

    @Override
    public void ignoreErrors() {
    }

    @Override
    public void saveMessageParameters(Map<String, String> parameters) {
    }

    @Override
    public CrossProcessState getCrossProcessState() {
        return NoOpCrossProcessState.INSTANCE;
    }

    @Override
    public TracedMethod startFlyweightTracer() {
        return NoOpTracedMethod.INSTANCE;
    }

    @Override
    public void finishFlyweightTracer(TracedMethod parent, long startInNanos, long finishInNanos, String className,
            String methodName, String methodDesc, String metricName, String[] rollupMetricNames) {
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        return AGENT_ATTRIBUTES;
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return USER_ATTRIBUTES;
    }

    @Override
    public boolean registerAsyncActivity(Object activityContext) {
        return false;
    }

    @Override
    public void provideHeaders(InboundHeaders headers) {
    }

    @Override
    public String getRequestMetadata() {
        return NoOpCrossProcessState.INSTANCE.getRequestMetadata();
    }

    @Override
    public void processRequestMetadata(String metadata) {
    }

    @Override
    public String getResponseMetadata() {
        return NoOpCrossProcessState.INSTANCE.getResponseMetadata();
    }

    @Override
    public void processResponseMetadata(String metadata) {
    }

    @Override
    public void processResponseMetadata(String responseMetadata, URI uri) {
    }

    @Override
    public void setWebRequest(ExtendedRequest request) {
    }

    static final class NoOpMap<K, V> implements Map<K, V> {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public V get(Object key) {
            return null;
        }

        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }

        @Override
        public Set<java.util.Map.Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

    }

    @Override
    public boolean markFirstByteOfResponse() {
        return false;
    }

    @Override
    public boolean markLastByteOfResponse() {
        return false;
    }

    @Override
    public void markResponseAtTxaEnd() {
    }

    @Override
    public boolean markResponseSent() {
        return false;
    }

    @Override
    public TracedActivity createAndStartTracedActivity() {
        return NoOpSegment.INSTANCE;
    }

    @Override
    public Token getToken() {
        return NoOpToken.INSTANCE;
    }

    @Override
    public Segment startSegment(String segmentName) {
        return NoOpSegment.INSTANCE;
    }

    @Override
    public Segment startSegment(String category, String segmentName) {
        return NoOpSegment.INSTANCE;
    }

    @Override
    public void expireAllTokens() {
    }

    @Override
    public boolean clearTransaction() {
      return false;
    }

  @Override
    public DistributedTracePayload createDistributedTracePayload() {
        return NoOpDistributedTracePayload.INSTANCE;
    }

    @Override
    public void acceptDistributedTracePayload(String payload) {
    }

    @Override
    public void acceptDistributedTracePayload(DistributedTracePayload payload) {
    }

    @Override
    public void insertDistributedTraceHeaders(Headers headers) {
    }

    @Override
    public void acceptDistributedTraceHeaders(TransportType transportType, Headers headers) {
    }

    @Override
    public Object getSecurityMetaData() {
        return new Object();
    }

    @Override
    public void setTransportType(TransportType transportType) {
    }
}
