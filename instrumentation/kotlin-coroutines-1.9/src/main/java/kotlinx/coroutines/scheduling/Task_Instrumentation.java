package kotlinx.coroutines.scheduling;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "kotlinx.coroutines.scheduling.TaskImpl", type = MatchType.BaseClass)
abstract class Task_Instrumentation {

//    @NewField
//    public Token token;
//
//    public Task_Instrumentation(Runnable r, long submissionTime, boolean taskContext) {
//
//    }

    @Trace
    public void run() {
        Weaver.callOriginal();
    }

}

