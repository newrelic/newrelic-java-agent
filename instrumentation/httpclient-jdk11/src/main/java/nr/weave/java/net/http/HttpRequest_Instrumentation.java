package nr.weave.java.net.http;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpclient.Java11HttpClientUtil;

import java.net.http.HttpRequest;

import static com.nr.agent.instrumentation.httpclient.Java11HttpClientUtil.*;


@Weave(originalName = "java.net.http.HttpRequest", type = MatchType.BaseClass)
public abstract class HttpRequest_Instrumentation {

    @Weave(originalName = "java.net.http.HttpRequest$Builder", type = MatchType.Interface)
    public static class HttpRequestBuilder_Instrumentation {
        public HttpRequest build() {
            Object thisBuilder = this;
            if (thisBuilder instanceof HttpRequest.Builder) {
                addOutboundHeaders((HttpRequest.Builder) thisBuilder);
            }
            return Weaver.callOriginal();
        }
    }
}
