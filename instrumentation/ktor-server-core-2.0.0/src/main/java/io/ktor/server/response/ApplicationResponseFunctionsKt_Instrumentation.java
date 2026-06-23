package io.ktor.server.response;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.ktor.server.KtorExtendedResponse;
import io.ktor.http.URLBuilder;
import io.ktor.server.application.ApplicationCall;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import io.ktor.http.HttpStatusCode;
import kotlin.jvm.functions.Function1;
import io.ktor.http.ContentType;
import io.ktor.http.content.OutgoingContent;

@Weave(originalName = "io.ktor.server.response.ApplicationResponseFunctionsKt")
public class ApplicationResponseFunctionsKt_Instrumentation {

    @Trace
    public static Object respondRedirect(ApplicationCall call, String url, boolean b, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    @Trace
    public static Object respondRedirect(ApplicationCall call, boolean b, Function1<? super URLBuilder, Unit> function1, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    @Trace
    public static Object respondText(ApplicationCall call, String url, ContentType contentType, HttpStatusCode status, Function1<? super OutgoingContent, Unit> function1, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    @Trace
    public static Object respondText(ApplicationCall call, ContentType contentType, HttpStatusCode status, Function1<? super Continuation<? super String>, ? extends Object> function1, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    @Trace
    public static Object respondBytes(ApplicationCall call, ContentType contentType, HttpStatusCode status, Function1<? super Continuation<? super byte[]>, ? extends Object> function1, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    @Trace
    public static Object respondBytes(ApplicationCall call, byte[] bytes, ContentType contentType, HttpStatusCode status, Function1<? super OutgoingContent, Unit> function1, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    @Trace
    public static Object respondBytesWriter(ApplicationCall call, ContentType contentType, HttpStatusCode status, Long contentLength, kotlin.jvm.functions.Function2<? super io.ktor.utils.io.ByteWriteChannel, ? super Continuation<? super Unit>, ? extends Object> function2, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

}
