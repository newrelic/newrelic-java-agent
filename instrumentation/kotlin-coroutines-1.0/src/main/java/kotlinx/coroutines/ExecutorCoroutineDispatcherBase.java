package kotlinx.coroutines;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import kotlin.Unit;

@Weave(type=MatchType.BaseClass)
public abstract class ExecutorCoroutineDispatcherBase {

    @Trace(dispatcher=true)
    public abstract void scheduleResumeAfterDelay(long timeInMS, CancellableContinuation<? super Unit> f);
    @Trace(dispatcher=true)
      public abstract DisposableHandle invokeOnTimeout(long ms, Runnable r);
    @Trace(dispatcher=true)
      private ScheduledFuture<?> scheduleBlock(Runnable r, long ms, TimeUnit tu) {
          return Weaver.callOriginal();
      }
}
