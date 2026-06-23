package io.ktor.server.cio;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import io.ktor.http.cio.HttpHeadersMap;
import java.util.Collection;
import java.util.Collections;

class CIORequestHeaders implements Headers {

    private final HttpHeadersMap headers;

    CIORequestHeaders(HttpHeadersMap headers) {
        this.headers = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        CharSequence value = headers.get(name);
        return value != null ? value.toString() : null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        String value = getHeader(name);
        if (value != null) {
            return Collections.singletonList(value);
        }
        return Collections.emptyList();
    }

    @Override
    public void setHeader(String name, String value) {
    }

    @Override
    public void addHeader(String name, String value) {
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.emptyList();
    }

    @Override
    public boolean containsHeader(String name) {
        return getHeader(name) != null;
    }
}
