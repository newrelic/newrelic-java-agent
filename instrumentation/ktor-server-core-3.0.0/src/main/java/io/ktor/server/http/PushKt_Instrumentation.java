package io.ktor.server.http;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.ktor.http.Parameters;
import io.ktor.server.application.ApplicationCall;
import io.ktor.server.response.ResponsePushBuilder;
import kotlin.jvm.functions.Function1;
import kotlin.Unit;

@Weave(originalName = "io.ktor.server.http.PushKt")
public class PushKt_Instrumentation {

    @Trace
    public static void push(ApplicationCall call, String pathAndQuery) {
        Weaver.callOriginal();
    }

    public static void push(ApplicationCall call, String pathAndQuery, Parameters params) {
        Weaver.callOriginal();
    }

    public static void push(ApplicationCall call, Function1<? super ResponsePushBuilder, Unit> function1) {
        Weaver.callOriginal();
    }
}
