/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.util.concurrent;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import util.TokenAwareRunnable;
import util.TokenDelegateExecutor;

@Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture")
public class CompletableFuture_Instrumentation<T> {

    @Weave(type = MatchType.BaseClass, originalName = "java.util.concurrent.CompletableFuture$Async")
    abstract static class Async extends ForkJoinTask<Void>
            implements Runnable, CompletableFuture.AsynchronousCompletionTask {
        public final Void getRawResult() {
            return null;
        }

        public final void setRawResult(Void v) {
        }

        public final void run() {
            exec();
        }
    }

    private static boolean noParallelism(Executor e) {
        return (e == ForkJoinPool.commonPool() &&
                ForkJoinPool.getCommonPoolParallelism() <= 1);
    }

    private static Executor useTokenDelegateExecutor(Executor e) {
        if (null == e || e instanceof TokenDelegateExecutor) {
            return e;
        } else {
            return new TokenDelegateExecutor(e);
        }
    }

    static void execAsync(Executor e, CompletableFuture_Instrumentation.Async r) {
      if (noParallelism(e)) {
        new Thread(new TokenAwareRunnable(r)).start();
      } else {
        Executor tde = useTokenDelegateExecutor(e);
        if (null != tde) {
          tde.execute(r);
        }
      }
    }

}
