/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.opentelemetry;

import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;

import java.net.URI;

final class OpenTelemetryTransaction implements Transaction {

    private static final OpenTelemetryTransaction INSTANCE = new OpenTelemetryTransaction();

    private OpenTelemetryTransaction() {
    }

    static OpenTelemetryTransaction getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category, String... parts) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "setTransactionName");
        return false;
    }

    @Override
    public boolean isTransactionNameSet() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "isTransactionNameSet");
        return false;
    }

    @Override
    public String getTransactionName() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "getTransactionName");
        return "";
    }

    @Override
    public TracedMethod getLastTracer() {
        return OpenTelemetryTracedMethod.getInstance();
    }

    @Override
    public TracedMethod getTracedMethod() {
        return OpenTelemetryTracedMethod.getInstance();
    }

    @Override
    public void ignore() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "ignore");
    }

    @Override
    public void ignoreApdex() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "ignoreApdex");
    }

    @Override
    public String getRequestMetadata() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "getRequestMetadata");
        return null;
    }

    @Override
    public void processRequestMetadata(String requestMetadata) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "processRequestMetadata");
    }

    @Override
    public String getResponseMetadata() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "getResponseMetadata");
        return null;
    }

    @Override
    public void processResponseMetadata(String responseMetadata) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "processResponseMetadata");
    }

    @Override
    public void processResponseMetadata(String responseMetadata, URI uri) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "processResponseMetadata");
    }

    @Override
    public void setWebRequest(ExtendedRequest request) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "setWebRequest");
    }

    @Override
    public void setWebResponse(Response response) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "setWebRequest");
    }

    @Override
    public boolean markResponseSent() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "markResponseSent");
        return false;
    }

    @Override
    public boolean isWebTransaction() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "isWebTransaction");
        return false;
    }

    @Override
    public void ignoreErrors() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "ignoreErrors");
    }

    @Override
    public void convertToWebTransaction() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "convertToWebTransaction");
    }

    @Override
    public void addOutboundResponseHeaders() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "addOutboundResponseHeaders");
    }

    @Override
    public Token getToken() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "getToken");
        return NoOpToken.getInstance();
    }

    @Override
    public Segment startSegment(String segmentName) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "startSegment");
        return NoOpSegment.getInstance();
    }

    @Override
    public Segment startSegment(String category, String segmentName) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "startSegment");
        return NoOpSegment.getInstance();
    }

    @Override
    public DistributedTracePayload createDistributedTracePayload() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "createDistributedTracePayload");
        return NoOpDistributedTracePayload.getInstance();
    }

    @Override
    public void acceptDistributedTracePayload(String payload) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "acceptDistributedTracePayload");
    }

    @Override
    public void acceptDistributedTracePayload(DistributedTracePayload payload) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "acceptDistributedTracePayload");
    }

    @Override
    public void insertDistributedTraceHeaders(Headers headers) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "insertDistributedTraceHeaders");
    }

    @Override
    public void acceptDistributedTraceHeaders(TransportType transportType, Headers headers) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "acceptDistributedTraceHeaders");
    }

    @Override
    public Object getSecurityMetaData() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Transaction", "getSecurityMetadata");
        return new Object();
    }

}
