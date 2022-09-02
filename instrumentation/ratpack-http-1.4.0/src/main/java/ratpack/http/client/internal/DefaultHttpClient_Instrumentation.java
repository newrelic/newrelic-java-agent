package ratpack.http.client.internal;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import nr.ratpack.instrumentation.RatpackHttpUtil;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;

import java.net.URI;

import static nr.ratpack.instrumentation.RatpackHttpUtil.RATPACK;

@Weave(originalName = "ratpack.http.client.internal.DefaultHttpClient")
public class DefaultHttpClient_Instrumentation {

    public Promise<ReceivedResponse> request(URI uri, Action<? super RequestSpec> action) {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment(RATPACK, "request");
            // override action
            action = action.prepend(RatpackHttpUtil.addHeaders(segment));
            Promise<ReceivedResponse> promise = Weaver.callOriginal();
            return promise.wiretap(RatpackHttpUtil.instrument(uri, segment, ReceivedResponse::getHeaders));
    }

    public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> action) {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment(RATPACK, "requestStream");
            // override action
            action = action.prepend(RatpackHttpUtil.addHeaders(segment));
            Promise<StreamedResponse> promise = Weaver.callOriginal();
            return promise.wiretap(RatpackHttpUtil.instrument(uri, segment, StreamedResponse::getHeaders));
    }
}