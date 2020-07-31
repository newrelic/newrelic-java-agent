/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.util.Obfuscator;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper that deobfuscates New Relic transaction headers.
 */
public class DeobfuscatedInboundHeaders extends ExtendedInboundHeaders {
    private InboundHeaders delegate;
    private String encodingKey;

    /**
     * @param headers     inbound headers delegate to wrap. Must not be null.
     * @param encodingKey
     */
    public DeobfuscatedInboundHeaders(InboundHeaders headers, String encodingKey) {
        this.delegate = headers;
        this.encodingKey = encodingKey;
    }

    @Override
    public HeaderType getHeaderType() {
        return delegate.getHeaderType();
    }

    @Override
    public String getHeader(String name) {
        if (encodingKey == null && !isDistributedTraceHeader(name)) {
            return null;
        }

        if (HeadersUtil.NEWRELIC_HEADERS.contains(name)) {
            String obfuscatedValue = getObfuscatedValue(name);
            if (obfuscatedValue == null) {
                return null;
            }
            return Obfuscator.deobfuscateNameUsingKey(obfuscatedValue, encodingKey);
        }

        return getObfuscatedValue(name);
    }

    private String getObfuscatedValue(String name) {
        String obfuscatedValue = delegate.getHeader(name);
        if (obfuscatedValue == null) {
            obfuscatedValue = delegate.getHeader(name.toUpperCase());
        }
        if (obfuscatedValue == null) {
            obfuscatedValue = delegate.getHeader(name.toLowerCase());
        }
        return obfuscatedValue;
    }

    @Override
    public List<String> getHeaders(String name) {
        if (!(delegate instanceof ExtendedInboundHeaders)) {
            return null;
        }
        ExtendedInboundHeaders extendedDelegate = (ExtendedInboundHeaders) delegate;

        if (encodingKey == null && !isDistributedTraceHeader(name)) {
            return null;
        }

        if (HeadersUtil.NEWRELIC_HEADERS.contains(name)) {
            List<String> values = getObfuscatedValues(name, extendedDelegate);
            if (values == null) {
                return null;
            }

            List<String> obfuscatedValues = new ArrayList<>(values.size());
            for (String value : values) {
                obfuscatedValues.add(Obfuscator.deobfuscateNameUsingKey(value, encodingKey));
            }

            return obfuscatedValues;
        }

        return getObfuscatedValues(name, extendedDelegate);
    }

    private List<String> getObfuscatedValues(String name, ExtendedInboundHeaders extendedDelegate) {
        List<String> values = extendedDelegate.getHeaders(name);
        if (values == null) {
            values = extendedDelegate.getHeaders(name.toUpperCase());
        }
        if (values == null) {
            values = extendedDelegate.getHeaders(name.toLowerCase());
        }
        return values;
    }

    private boolean isDistributedTraceHeader(String name) {
        return name.equalsIgnoreCase(HeadersUtil.NEWRELIC_TRACE_HEADER) ||
                name.equalsIgnoreCase(HeadersUtil.NEWRELIC_TRACE_MESSAGE_HEADER) ||
                name.equalsIgnoreCase(HeadersUtil.W3C_TRACEPARENT_HEADER) ||
                name.equalsIgnoreCase(HeadersUtil.W3C_TRACESTATE_HEADER);
    }
}
