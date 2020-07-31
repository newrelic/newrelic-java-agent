package com.newrelic;

import com.newrelic.agent.model.SpanEvent;

public interface SpanConverter<T> {
    T convert(SpanEvent event);
}
