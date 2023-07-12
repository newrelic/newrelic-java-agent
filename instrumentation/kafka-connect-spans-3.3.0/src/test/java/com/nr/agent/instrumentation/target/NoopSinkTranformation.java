package com.nr.agent.instrumentation.target;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.transforms.Transformation;

import java.util.Map;

public class NoopSinkTranformation implements Transformation<SinkRecord> {
    @Override
    public SinkRecord apply(SinkRecord record) {
        return record;
    }

    @Override
    public ConfigDef config() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
