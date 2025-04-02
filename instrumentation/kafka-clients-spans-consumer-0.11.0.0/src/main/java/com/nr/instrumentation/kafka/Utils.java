package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.NewRelic;

public class Utils {
    public static final boolean DT_CONSUMER_ENABLED = NewRelic.getAgent().getConfig()
            .getValue("kafka.spans.distributed_trace.consumer_poll.enabled", false);
}
