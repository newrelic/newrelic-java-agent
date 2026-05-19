package io.ktor.server.response;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.ktor.server.KtorExtendedResponse;
import io.ktor.http.content.OutgoingContent;
import io.ktor.server.application.ApplicationCall;
import io.ktor.http.ContentType;
import io.ktor.http.HttpStatusCode;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.coroutines.Continuation;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import kotlin.Unit;

@Weave(originalName = "io.ktor.server.response.ApplicationResponseFunctionsJvmKt")
public class ApplicationResponseFunctionsJvmKt_Instrumentation {

    @Trace
    public static Object respondTextWriter(ApplicationCall call, ContentType contentType, HttpStatusCode status, Function2<? super Writer, ? super Continuation<? super Unit>, Object> function2, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    @Trace
    public static Object respondOutputStream(ApplicationCall call, ContentType contentType, HttpStatusCode status, Function2<? super OutputStream, ? super Continuation<? super Unit>, Object> function2, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    @Trace
    public static Object respondFile(ApplicationCall call, File baseDir, String fileName, Function1<? super OutgoingContent, Unit> function1, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    @Trace
    public static Object respondFile(ApplicationCall call, File file, Function1<? super OutgoingContent, Unit> function1, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    @Trace
    public static Object respondTextWriter(ApplicationCall call, ContentType contentType, HttpStatusCode status, Long contentLength, Function2<? super Writer, ? super Continuation<? super Unit>, Object> function2, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    @Trace
    public static Object respondOutputStream(ApplicationCall call, ContentType contentType, HttpStatusCode status, Long contentLength, Function2<? super OutputStream, ? super Continuation<? super Unit>, Object> function2, Continuation<? super Unit> continuation) {
        KtorExtendedResponse extendedResponse = new KtorExtendedResponse(call);
        NewRelic.getAgent().getTransaction().setWebResponse(extendedResponse);
        return Weaver.callOriginal();
    }

    
}
