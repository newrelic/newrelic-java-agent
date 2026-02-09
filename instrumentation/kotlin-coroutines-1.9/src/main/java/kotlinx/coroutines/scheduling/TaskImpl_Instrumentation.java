package kotlinx.coroutines.scheduling;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "kotlinx.coroutines.scheduling.TaskImpl")
class TaskImpl_Instrumentation extends Task {

    @NewField
    public Token token;

    public TaskImpl_Instrumentation(Runnable runnable, long timeout, boolean b) {

    }

    @Trace(dispatcher = true)
    public void run() {
        if(token != null) {
            token.linkAndExpire();
            token = null;
        }
        Weaver.callOriginal();
    }

}
