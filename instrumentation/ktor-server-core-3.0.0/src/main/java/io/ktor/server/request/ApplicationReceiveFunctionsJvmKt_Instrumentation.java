package io.ktor.server.request;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.ktor.server.application.ApplicationCall;
import kotlin.coroutines.Continuation;

import java.io.InputStream;

@Weave(originalName = "io.ktor.server.request.ApplicationReceiveFunctionsJvmKt")
public class ApplicationReceiveFunctionsJvmKt_Instrumentation {

    @Trace
    public static Object receiveStream(ApplicationCall call, Continuation<? super InputStream> continuation) {
        return Weaver.callOriginal();
    }
}
