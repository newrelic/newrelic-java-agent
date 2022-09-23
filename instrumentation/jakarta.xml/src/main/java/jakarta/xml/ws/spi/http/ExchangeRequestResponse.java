package jakarta.xml.ws.spi.http;

import java.security.Principal;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class ExchangeRequestResponse extends ExtendedRequest implements Response {

    private final HttpExchange exchange;

    public ExchangeRequestResponse(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public int getStatus() throws Exception {
        return 200;
    }

    @Override
    public String getStatusMessage() throws Exception {
        return null;
    }

    @Override
    public void setHeader(String name, String value) {
        exchange.getRequestHeaders().put(name, Arrays.asList(value));
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return exchange.getRequestURI();
    }

    @Override
    public String getHeader(String name) {
        return exchange.getRequestHeader(name);
    }

    @Override
    public String getRemoteUser() {
        Principal userPrincipal = exchange.getUserPrincipal();
        return userPrincipal == null ? null : userPrincipal.getName();
    }

    @Override
    public Enumeration getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getMethod() {
        return exchange.getRequestMethod();
    }

    @Override
    public List<String> getHeaders(String name) {
        return exchange.getRequestHeaders().get(name);
    }
}
