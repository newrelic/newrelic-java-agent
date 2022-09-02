package nr.ratpack.instrumentation;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

public class RatpackResponse implements Response {

    private final ratpack.http.Response response;

    public RatpackResponse(ratpack.http.Response response) {
        this.response = response;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        response.getHeaders().add(name, value);
    }

    @Override
    public int getStatus() throws Exception {
        return response.getStatus().getCode();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return response.getStatus().getMessage();
    }

    @Override
    public String getContentType() {
        return response.getHeaders().get(CONTENT_TYPE);
    }
}
