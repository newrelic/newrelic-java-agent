/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.http;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.collection.immutable.List;

import java.net.InetSocketAddress;

@Weave(originalName = "spray.http.HttpRequest")
public class HttpRequest_Instrumentation {

    @NewField
    public boolean headersAdded;

    @NewField
    public Segment segment;

    @NewField
    public InetSocketAddress remoteAddress;

    @NewField
    public boolean isSSL;

    public HttpRequest_Instrumentation withHeaders(final List<HttpHeader> headers) {
        return Weaver.callOriginal();
    }

    public List<HttpHeader> headers() {
        return Weaver.callOriginal();
    }

    public Uri uri() {
        return Weaver.callOriginal();
    }

    public HttpRequest_Instrumentation withEffectiveUri(final boolean securedConnection, final HttpHeaders.Host defaultHostHeader) {
        HttpRequest_Instrumentation result = Weaver.callOriginal();
        result.headersAdded = this.headersAdded;
        result.segment = this.segment;
        result.remoteAddress = this.remoteAddress;
        result.isSSL = this.isSSL;
        return result;
    }

}
