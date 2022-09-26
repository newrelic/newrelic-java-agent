package com.nr.agent.instrumentation

import com.newrelic.agent.bridge.{AgentBridge}
import com.nr.agent.instrumentation.TokenAwareRunnable._

import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level

final class TokenAwareRunnable(val delegate: Runnable) extends Runnable {
  private val tokenAndRefCount:AgentBridge.TokenAndRefCount = getThreadTokenAndRefCount
  logTokenInfo(tokenAndRefCount, "EC TokenAwareRunnable token info set")
  clearTxn()

  override def run(): Unit = {
    try if (delegate != null) {
      setThreadTokenAndRefCount(tokenAndRefCount)
      delegate.run()
    }
    finally {
      clearThreadTokenAndRefCountAndTxn(tokenAndRefCount)
    }
  }
}

object TokenAwareRunnable {
  def getThreadTokenAndRefCount: AgentBridge.TokenAndRefCount = {
    val activeToken = AgentBridge.activeToken.get
    val txn = AgentBridge.getAgent.getTransaction(false)
    if(activeToken != null) {
        if(activeToken.refCount == null) {
          activeToken.refCount = new AtomicInteger(1)
        } else {
          activeToken.refCount.incrementAndGet()
        }
        activeToken
    } else if (txn != null) {
      new AgentBridge.TokenAndRefCount(txn.getToken, AgentBridge.getAgent.getTracedMethod, new AtomicInteger(1))
    } else {
      null
    }
  }

  def clearTxn(): Unit = {
    AgentBridge.activeToken.remove()
    val txn = AgentBridge.getAgent.getTransaction(false)
    if(txn != null) {
      txn.clearTransaction()
      logFinest(s"Txn ${txn.hashCode()} cleared from Thread: ${Thread.currentThread().getName}")
    }
  }

  def setThreadTokenAndRefCount(tokenAndRefCount: AgentBridge.TokenAndRefCount): Unit = {
    if (tokenAndRefCount != null && tokenAndRefCount.token != null) {
      AgentBridge.activeToken.set(tokenAndRefCount)
      tokenAndRefCount.token.link
    }
  }

  def clearThreadTokenAndRefCountAndTxn(tokenAndRefCount: AgentBridge.TokenAndRefCount): Unit = {
    AgentBridge.activeToken.remove()
    if (tokenAndRefCount != null && tokenAndRefCount.refCount.decrementAndGet == 0) {
      tokenAndRefCount.token.expire
      tokenAndRefCount.token = null
    }
  }

  def logTokenInfo(tokenAndRefCount: AgentBridge.TokenAndRefCount, msg: String): Unit =
      if (AgentBridge.getAgent.getLogger.isLoggable(Level.FINEST)) {
        logFinest{
          val tokenMsg = if (tokenAndRefCount != null && tokenAndRefCount.token != null) {
            s"[${tokenAndRefCount.token}:${tokenAndRefCount.token.getTransaction}:${tokenAndRefCount.refCount.get}]"
          } else {
            "[Empty token]"
          }

          s"[${Thread.currentThread().getName}] ${tokenMsg}: token info ${msg}"
        }
     }

  def logFinest(msg: => String): Unit =
    if (AgentBridge.getAgent.getLogger.isLoggable(Level.FINEST)) {
      AgentBridge.getAgent.getLogger.log(Level.FINEST, msg)
    }

}
