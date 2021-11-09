package monix;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "monix.execution.Scheduler")
public class MonixScheduler_Instrumentation {
    public void execute(Runnable command) {
        command = new TokenAwareRunnable(command);
        Weaver.callOriginal();
    }
}
