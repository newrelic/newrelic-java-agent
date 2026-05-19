package io.ktor.server.http.content;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.ktor.server.routing.Route;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@Weave(originalName = "io.ktor.server.http.content.SinglePageApplicationKt")
public class SinglePageApplicationKt_Instrumentation {

    @Trace
    public static void singlePageApplication(Route route, Function1<? super SPAConfig, Unit> function1) {
        Weaver.callOriginal();
    }

    @Trace
    public static void ignoreFiles(SPAConfig spaConfig, Function1<? super String, Boolean> function1) {
        Weaver.callOriginal();
    }

    @Trace
    public static void angular(SPAConfig spaConfig, String filesPath) {
        Weaver.callOriginal();
    }

    @Trace
    public static void react(SPAConfig spaConfig, String filesPath) {
        Weaver.callOriginal();
    }

    @Trace
    public static void vue(SPAConfig spaConfig, String filesPath) {
        Weaver.callOriginal();
    }

    @Trace
    public static void ember(SPAConfig spaConfig, String filesPath) {
        Weaver.callOriginal();
    }

    @Trace
    public static void backbone(SPAConfig spaConfig, String filesPath) {
        Weaver.callOriginal();
    }
}
