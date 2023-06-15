/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.target;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

public class StubSourceTask extends SourceTask {

    private final String topic;
    private final List<SourceRecord> records;

    public StubSourceTask(String topic, String ... polledValues) {
        this.topic = topic;
        records = Arrays.stream(polledValues)
                .map(this::toRecord)
                .collect(Collectors.toList());
    }

    private SourceRecord toRecord(String s) {
        return new SourceRecord(emptyMap(), emptyMap(), topic, Schema.STRING_SCHEMA, s);
    }

    @Override
    public String version() {
        return null;
    }

    @Override
    public void start(Map<String, String> props) {

    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        Thread.sleep(100L);
        return records;
    }

    @Override
    public void stop() {

    }
}
