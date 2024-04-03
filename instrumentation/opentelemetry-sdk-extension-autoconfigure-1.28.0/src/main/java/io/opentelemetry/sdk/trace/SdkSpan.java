package io.opentelemetry.sdk.trace;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.opentelemetry.api.common.AttributeKey;

@Weave(type = MatchType.ExactClass)
final class SdkSpan {
    public <T> ReadWriteSpan setAttribute(AttributeKey<T> key, T value) {
        // None of the Span interfaces expose an accessor to get all of the attributes, so we have to intercept the setter
        switch (key.getType()) {
            case BOOLEAN:
                NewRelic.getAgent().getTracedMethod().addCustomAttribute(key.getKey(), (Boolean) value);
                break;
            case LONG:
            case DOUBLE:
                NewRelic.getAgent().getTracedMethod().addCustomAttribute(key.getKey(), (Number) value);
                break;
            case STRING:
                NewRelic.getAgent().getTracedMethod().addCustomAttribute(key.getKey(), (String) value);
                break;
        }
        return Weaver.callOriginal();
    }
}
