package com.nr.agent.instrumentation.httpclient50;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class WrappedResponseConsumer implements AsyncResponseConsumer {

    AsyncResponseConsumer origConsumer;
    Token token;
    public WrappedResponseConsumer (AsyncResponseConsumer origConsumer, Token token) {
        this.origConsumer = origConsumer;
        this.token = token;
    }

    @Override
    public void consumeResponse(HttpResponse response, EntityDetails entityDetails, HttpContext context, FutureCallback resultCallback)
            throws HttpException, IOException {
        if (origConsumer != null) origConsumer.consumeResponse(response, entityDetails, context, resultCallback);
    }

    @Override
    public void informationResponse(HttpResponse response, HttpContext context) throws HttpException, IOException {
        if (origConsumer != null) origConsumer.informationResponse(response, context);
    }

    @Override
    public void failed(Exception cause) {
        if (origConsumer != null) origConsumer.failed(cause);
    }

    @Override
    public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
        if (origConsumer != null) origConsumer.updateCapacity(capacityChannel);
    }

    @Override
    public void consume(ByteBuffer src) throws IOException {
        if (origConsumer != null) origConsumer.consume(src);
    }

    @Override
    @Trace(async = true)
    public void streamEnd(List<? extends Header> trailers) throws HttpException, IOException {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        if (origConsumer != null) origConsumer.streamEnd(trailers);
    }

    @Override
    public void releaseResources() {
        if (origConsumer != null) origConsumer.releaseResources();
    }
}
