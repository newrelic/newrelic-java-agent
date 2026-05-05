package kotlinx.coroutines.scheduling;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_17.NRRunnable;
import com.newrelic.instrumentation.kotlin.coroutines_17.Utils;

@Weave(originalName = "kotlinx.coroutines.scheduling.CoroutineScheduler")
public class CoroutineScheduler_Instrumentation {

    public void dispatch(Runnable block, TaskContext taskContext, boolean tailDispatch) {
        NRRunnable wrapper = Utils.getRunnableWrapper(block);
        if (wrapper != null) {
            block =  wrapper;
        }
        Weaver.callOriginal();
    }
}
