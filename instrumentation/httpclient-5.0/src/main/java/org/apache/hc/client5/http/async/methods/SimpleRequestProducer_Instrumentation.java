package org.apache.hc.client5.http.async.methods;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;

@Weave(type=MatchType.ExactClass, originalName = "org.apache.hc.client5.http.async.methods.SimpleRequestProducer")
public class SimpleRequestProducer_Instrumentation {

    @NewField
    public final HttpRequest nrRequest;

    SimpleRequestProducer_Instrumentation(final SimpleHttpRequest request, final AsyncEntityProducer entityProducer) {
        nrRequest = request;
    }
}
