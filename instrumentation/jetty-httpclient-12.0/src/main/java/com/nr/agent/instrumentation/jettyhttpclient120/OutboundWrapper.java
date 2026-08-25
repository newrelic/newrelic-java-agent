/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jettyhttpclient120;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import java.util.function.Consumer;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;

public class OutboundWrapper implements OutboundHeaders {

    private final Request delegate;

    public OutboundWrapper(Request request) {
        this.delegate = request;
    }

    @Override
    public void setHeader(final String name, final String value) {
        delegate.headers(new Consumer<HttpFields.Mutable>() {
            @Override
            public void accept(HttpFields.Mutable httpFields) {
                httpFields.put(new HttpField(name, value));
            }
        });
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
