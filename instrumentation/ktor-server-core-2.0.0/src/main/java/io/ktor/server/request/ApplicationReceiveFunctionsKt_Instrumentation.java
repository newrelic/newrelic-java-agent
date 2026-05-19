package io.ktor.server.request;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.ktor.server.KtorExtendedRequest;
import io.ktor.http.Parameters;
import io.ktor.http.content.MultiPartData;
import io.ktor.server.application.ApplicationCall;
import kotlin.coroutines.Continuation;
import kotlin.reflect.KClass;
import io.ktor.utils.io.ByteReadChannel;

@Weave(originalName = "io.ktor.server.request.ApplicationReceiveFunctionsKt")
public class ApplicationReceiveFunctionsKt_Instrumentation {

    @Trace(dispatcher = true)
    public static <T> Object receive(ApplicationCall call, KClass<T> tkClass, Continuation<? super T> continuation) {
        KtorExtendedRequest extendedRequest = new KtorExtendedRequest(call);
        NewRelic.getAgent().getTransaction().setWebRequest(extendedRequest);
        return Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public static <T> Object receiveOrNull(ApplicationCall call, KClass<T> tkClass, Continuation<? super T> continuation) {
        KtorExtendedRequest extendedRequest = new KtorExtendedRequest(call);
        NewRelic.getAgent().getTransaction().setWebRequest(extendedRequest);
        return Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public static Object receiveText(ApplicationCall call, Continuation<? super String> continuation) {
        KtorExtendedRequest extendedRequest = new KtorExtendedRequest(call);
        NewRelic.getAgent().getTransaction().setWebRequest(extendedRequest);
        return Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public static Object receiveChannel(ApplicationCall call, Continuation<? super ByteReadChannel> continuation) {
        KtorExtendedRequest extendedRequest = new KtorExtendedRequest(call);
        NewRelic.getAgent().getTransaction().setWebRequest(extendedRequest);
        return Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public static Object receiveMultipart(ApplicationCall call, Continuation<? super MultiPartData> continuation) {
        KtorExtendedRequest extendedRequest = new KtorExtendedRequest(call);
        NewRelic.getAgent().getTransaction().setWebRequest(extendedRequest);
        return Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public static Object receiveParameters(ApplicationCall call, Continuation<? super Parameters> continuation) {
        KtorExtendedRequest extendedRequest = new KtorExtendedRequest(call);
        NewRelic.getAgent().getTransaction().setWebRequest(extendedRequest);
        return Weaver.callOriginal();
    }
}
