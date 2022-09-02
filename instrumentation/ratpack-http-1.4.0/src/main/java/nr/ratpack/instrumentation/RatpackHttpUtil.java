package nr.ratpack.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;
import ratpack.exec.Result;
import ratpack.func.Action;
import ratpack.http.Headers;
import ratpack.http.client.RequestSpec;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

public class RatpackHttpUtil {

   public static final String RATPACK = "RatpackHttpClient";

   public static <R> Action<Result<R>> instrument(final URI uri, final Segment segment,
                                                  Function<R, Headers> headersSupplier) {
      return result -> {
         try {
            if (result.isSuccess()) {
               segment.reportAsExternal(
                       HttpParameters
                               .library(RATPACK)
                               .uri(uri)
                               .procedure("success")
                               .inboundHeaders(new RatpackInboundHeaders(headersSupplier.apply(result.getValue())))
                               .build());
            } else {
               segment.reportAsExternal(HttpParameters
                       .library(RATPACK)
                       .uri(uri)
                       .procedure("error")
                       .noInboundHeaders()
                       .build());
            }

            segment.end();
         } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
         }
      };

   }

   public static Action<? super RequestSpec> addHeaders(Segment segment) {
       return (Action<RequestSpec>) requestSpec -> {
           segment.addOutboundRequestHeaders(new RatpackHttpHeaders(requestSpec.getHeaders()));
       };
   }
}
