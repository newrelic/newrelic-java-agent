package io.opentelemetry.instrumentation.api.instrumenter;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.otelapi.Helper;
//import io.opentelemetry.context.Context;

@Weave(type = MatchType.BaseClass, originalName = "io.opentelemetry.instrumentation.api.instrumenter.Instrumenter")
//@Weave(type = MatchType.BaseClass, originalName = "io.opentelemetry.javaagent.shaded.instrumentation.api.instrumenter.Instrumenter")
public class Instrumenter_Instrumentation<REQUEST, RESPONSE> {

    @WeaveAllConstructors
    Instrumenter_Instrumentation() {
        Helper.foo("constructor");
    }

//    public Context start(Context parentContext, REQUEST request) {
//        Helper.foo("start");
//        return Weaver.callOriginal();
//    }
//
//    public void end(Context context, REQUEST request, RESPONSE response, Throwable error) {
//        Helper.foo("end");
//        Weaver.callOriginal();
//    }

}
