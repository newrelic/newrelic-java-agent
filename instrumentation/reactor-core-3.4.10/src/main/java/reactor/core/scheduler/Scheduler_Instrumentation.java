package reactor.core.scheduler;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.NRRunnableWrapper;
import com.nr.instrumentation.reactor.ReactorUtils;
import reactor.core.Disposable;

import java.util.concurrent.TimeUnit;

@Weave(originalName = "reactor.core.scheduler.Scheduler", type = MatchType.Interface)
public class Scheduler_Instrumentation {

    public Disposable schedule(Runnable task) {
        NRRunnableWrapper wrapper = ReactorUtils.getRunnableWrapper(task);
        if (wrapper != null) {
            task = wrapper;
        }
        return Weaver.callOriginal();
    }

    public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
        NRRunnableWrapper wrapper = ReactorUtils.getRunnableWrapper(task);
        if (wrapper != null) {
            task = wrapper;
        }
        return Weaver.callOriginal();
    }

    @Weave(originalName = "reactor.core.scheduler.Scheduler$Worker", type = MatchType.Interface)
    public static class Worker_Instrumentation {

        public Disposable schedule(Runnable task) {
            NRRunnableWrapper wrapper = ReactorUtils.getRunnableWrapper(task);
            if (wrapper != null) {
                task = wrapper;
            }
            return Weaver.callOriginal();
        }

        public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
            NRRunnableWrapper wrapper = ReactorUtils.getRunnableWrapper(task);
            if (wrapper != null) {
                task = wrapper;
            }
            return Weaver.callOriginal();
        }

    }
}
