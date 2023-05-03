package com.nr.agent.instrumentation.httpclient50;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

public class WrappedFutureCallback<T> implements FutureCallback<T> {

    HttpRequest request;
    FutureCallback origCallback;

    public WrappedFutureCallback (HttpRequest request, FutureCallback origCallback) {
        this.request = request;
        this.origCallback = origCallback;
    }

    @Override
    @Trace(async = true)
    public void completed(T response) {
        try {
            InstrumentationUtils.processResponse(request.getUri(), (SimpleHttpResponse)response);
        } catch (URISyntaxException e) {
            // TODO throw new IOException(e);
            // TODO can this happen now?  perhaps in one of the overload methods?
        }
        if (origCallback != null) origCallback.completed(response);
    }

    @Override
    @Trace(async = true)
    public void failed(Exception ex) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "I done intercepted it: failed");
        InstrumentationUtils.handleUnknownHost(ex);
        if (origCallback != null) origCallback.failed(ex);
    }

    @Override
    @Trace(async = true)
    public void cancelled() {
        // TODO handle cancellation
        NewRelic.getAgent().getLogger().log(Level.INFO, "I done intercepted it: cancelled");
        if (origCallback != null) origCallback.cancelled();
    }

}
