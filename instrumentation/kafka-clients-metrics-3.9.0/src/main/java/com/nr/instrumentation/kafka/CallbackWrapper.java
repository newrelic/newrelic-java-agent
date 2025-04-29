/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.NewRelic;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.HashMap;
import java.util.Map;

public class CallbackWrapper implements Callback {

    private final Callback callback;
    private final String topic;

    public CallbackWrapper(Callback callback, String topic) {
        this.callback = callback;
        this.topic = topic;
    }

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        try {
            if (exception != null) {
                Map<String, Object> atts = new HashMap<>();
                atts.put("topic_name", topic);
                NewRelic.noticeError(exception, atts);
            }
        } catch (Throwable t) {
        }

        this.callback.onCompletion(metadata, exception);
    }

}
