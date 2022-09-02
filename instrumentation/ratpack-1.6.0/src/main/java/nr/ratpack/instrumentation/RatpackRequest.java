package nr.ratpack.instrumentation;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import io.netty.handler.codec.http.cookie.Cookie;
import ratpack.http.Request;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RatpackRequest extends ExtendedRequest {

    private final Request request;

    public RatpackRequest(Request request) {
        this.request = request;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return request.getHeaders().get(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return request.getHeaders().getAll(name);
    }

    @Override
    public String getMethod() {
        return request.getMethod().getName();
    }

    @Override
    public String getRequestURI() {
        return request.getUri();
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public Enumeration getParameterNames() {
        return new Enumeration<String>() {

            final Iterator<String> iterator = request.getQueryParams().keySet().iterator();

            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };
    }

    @Override
    public String[] getParameterValues(String name) {
        return new String[]{request.getQueryParams().get(name)};
    }

    @Override
    public Object getAttribute(String name) {
        // Servlet stuff. No Servlet, no attributes.
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        Set<Cookie> cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.name().equals(name)) {
                return cookie.value();
            }
        }

        return null;
    }
}
