/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka.spans;

import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

public class ProducerRecordWrapper extends ExtendedInboundHeaders implements Headers {
    private ProducerRecord record;

    public ProducerRecordWrapper(ProducerRecord record) {
        super();
        this.record = record;
    }

    @Override
    public void setHeader(String name, String value) {
        addHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        if (value == null) {
            return;
        }
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Collection<String> getHeaderNames() {
        Collection<String> names = new CopyOnWriteArraySet<>();
        for (Header header: record.headers()) {
            names.add(header.key());
        }
        return names;
    }

    @Override
    public boolean containsHeader(String name) {
        boolean hasHeader = false;
        for (Header header: record.headers().headers(name)) {
            hasHeader = true;
            break;
        }
        return hasHeader;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

    @Override
    public String getHeader(String name) {
        for (Header header: record.headers().headers(name)) {
            if (header.value() != null) {
                return new String(header.value(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Override
    public List<String> getHeaders(String name) {
        List<String> headers = new ArrayList<>();
        for (Header header: record.headers().headers(name)) {
            if (header.value() != null) {
                headers.add(new String(header.value(), StandardCharsets.UTF_8));
            }
        }
        return headers;
    }
}
