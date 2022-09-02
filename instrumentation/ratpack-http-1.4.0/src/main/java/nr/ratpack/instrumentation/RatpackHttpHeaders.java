package nr.ratpack.instrumentation;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import ratpack.http.MutableHeaders;

public class RatpackHttpHeaders implements OutboundHeaders {
    final MutableHeaders headers;

    public RatpackHttpHeaders(MutableHeaders headers) {
        this.headers = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.add(name, value);
    }
}
