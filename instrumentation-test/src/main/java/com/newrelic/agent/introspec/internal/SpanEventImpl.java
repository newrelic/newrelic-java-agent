/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.introspec.SpanEvent;

import java.util.Map;

public class SpanEventImpl implements SpanEvent {

    private final com.newrelic.agent.model.SpanEvent spanEvent;

    public SpanEventImpl(com.newrelic.agent.model.SpanEvent spanEvent) {
        this.spanEvent = spanEvent;
    }

    /**
     * This method is just to facilitate testing
     *
     * @return String representing Span ID
     */
    @Override
    public String getGuid() {
        return spanEvent.getGuid();
    }

    @Override
    public String getName() {
        return spanEvent.getName();
    }

    @Override
    public float duration() {
        return spanEvent.getDuration();
    }

    @Override
    public String traceId() {
        return spanEvent.getTraceId();
    }

    @Override
    public String parentId() {
        return spanEvent.getParentId();
    }

    @Override
    public String category() {
        return (String) spanEvent.getIntrinsics().get("category");
    }

    @Override
    public String getHttpUrl() {
        return (String) spanEvent.getAgentAttributes().get("http.url");
    }

    @Override
    public String getHttpMethod() {
        return (String) spanEvent.getAgentAttributes().get("http.method");
    }

    @Override
    public String getHttpComponent() {
        return (String) spanEvent.getIntrinsics().get("component");
    }

    @Override
    public String getTransactionId() {
        return (String) spanEvent.getIntrinsics().get("transactionId");
    }

    @Override
    public Integer getStatusCode() {
        return (Integer) spanEvent.getAgentAttributes().get("http.statusCode");
    }

    @Override
    public String getStatusText() {
        return (String) spanEvent.getAgentAttributes().get("http.statusText");
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        return spanEvent.getAgentAttributes();
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return spanEvent.getUserAttributesCopy();
    }
}
