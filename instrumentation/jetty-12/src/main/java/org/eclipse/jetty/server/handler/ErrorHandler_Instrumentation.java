package org.eclipse.jetty.server.handler;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import static org.eclipse.jetty.server.handler.ErrorHandler.ERROR_EXCEPTION;

@Weave(originalName = "org.eclipse.jetty.server.handler.ErrorHandler", type = MatchType.ExactClass)
public class ErrorHandler_Instrumentation {
    public boolean handle(Request request, Response response, Callback callback) {
        Object attribute = request.getAttribute(ERROR_EXCEPTION);
        final Throwable throwable = (attribute != null) ? (Throwable) attribute : null;

        // call the original implementation
        try {
            return Weaver.callOriginal();
        } finally {
            if (throwable != null) {
                NewRelic.noticeError(throwable);
            }
        }

    }
}
