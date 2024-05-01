package io.opentelemetry.sdk.trace;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.opentelemetry.api.common.AttributeKey;

/**
 * None of the Span interfaces expose an accessor to get all attributes, so we have to intercept the
 * `setAttribute` method to observe attributes and add them to our tracers.
 *
 * We could call `ReadableSpan.toSpanData()` when the span finishes, but that introduces unnecessary object allocation.
 */
@Weave(type = MatchType.ExactClass)
final class SdkSpan {
    public <T> ReadWriteSpan setAttribute(AttributeKey<T> key, T value) {
        AttributeHelper.setAttribute(NewRelic.getAgent(), key, value);
        return Weaver.callOriginal();
    }
}
