package nr.ratpack.instrumentation;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import ratpack.http.Headers;

import java.util.List;

public class RatpackInboundHeaders extends ExtendedInboundHeaders {
    final Headers headers;

    public RatpackInboundHeaders(Headers headers) {
        this.headers = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return headers.getAll(name);
    }
}
