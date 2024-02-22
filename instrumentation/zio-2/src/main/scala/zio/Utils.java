/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package zio;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;

public class Utils {

  public static AgentBridge.TokenAndRefCount getThreadTokenAndRefCount() {
    AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
    // Used to be that if the tokenAndRefCount is not null, we increment the counter and then return the tokenAndRefCount
    if (tokenAndRefCount == null) {
      Transaction tx = AgentBridge.getAgent().getTransaction(false);
      if (tx != null) {
        tokenAndRefCount = new AgentBridge.TokenAndRefCount(tx.getToken(),
                AgentBridge.getAgent().getTracedMethod(), new AtomicInteger(1));
      }
    } else {
      tokenAndRefCount.refCount.incrementAndGet();
    }

    return tokenAndRefCount;
  }

  public static void setThreadTokenAndRefCount(AgentBridge.TokenAndRefCount tokenAndRefCount) {
    if (tokenAndRefCount != null) {
      AgentBridge.activeToken.set(tokenAndRefCount);
      tokenAndRefCount.token.link();
    }
  }

  public static void clearThreadTokenAndRefCountAndTxn(AgentBridge.TokenAndRefCount tokenAndRefCount) {
    AgentBridge.activeToken.remove();
    if (tokenAndRefCount != null && tokenAndRefCount.refCount.decrementAndGet() <= 0) {
      tokenAndRefCount.token.expire();
      tokenAndRefCount.token = null;
    }
  }

  public static void logTokenInfo(AgentBridge.TokenAndRefCount tokenAndRefCount, String msg) {
    if (AgentBridge.getAgent().getLogger().isLoggable(Level.FINEST)) {
      String tokenMsg = (tokenAndRefCount != null && tokenAndRefCount.token != null)
              ? String.format("[%s:%s:%d]", tokenAndRefCount.token, tokenAndRefCount.token.getTransaction(),
              tokenAndRefCount.refCount.get())
              : "[Empty token]";
      AgentBridge.getAgent().getLogger().log(Level.FINEST, MessageFormat.format("{0}: token info {1}", tokenMsg, msg));
    }
  }

}