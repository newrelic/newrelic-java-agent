/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package zio;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "zio.Executor", type = MatchType.BaseClass)
public class ZIOExecutor_Instrumentation {

  public boolean submit(Runnable runnable, Unsafe unsafe) {
    runnable = new TokenAwareRunnable(runnable);
    return Weaver.callOriginal();
  }
}
