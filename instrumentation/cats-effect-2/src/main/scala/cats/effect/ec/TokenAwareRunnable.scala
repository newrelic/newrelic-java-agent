package cats.effect.ec

import cats.effect.ec.TokenAwareRunnable._
import com.newrelic.agent.bridge.{AgentBridge, Transaction}

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
    var tokenAndRefCount = AgentBridge.activeToken.get
    if (tokenAndRefCount == null) {
      val tx = AgentBridge.getAgent.getTransaction(false)
      if (tx != null) tokenAndRefCount = new AgentBridge.TokenAndRefCount(tx.getToken, AgentBridge.getAgent.getTracedMethod, new AtomicInteger(1))
    }
    else tokenAndRefCount.refCount.incrementAndGet
    tokenAndRefCount
  }

  def clearTxn(): Unit = {
    AgentBridge.activeToken.remove()
    val txn = AgentBridge.getAgent.getTransaction(false)
    if(txn != null) {
      val txnCleared = txn.clearTransaction()
      if (AgentBridge.getAgent.getLogger.isLoggable(Level.FINEST)) {
        AgentBridge.getAgent.getLogger.log(Level.FINEST, s"[IOTick] Txn ${txn.hashCode()} cleared from Thread: ${Thread
          .currentThread().getName}")
        AgentBridge.getAgent.getLogger.log(Level.FINEST,
          s"[IOTick] Txn ${txn.hashCode()} ${if(!txnCleared) "not"} cleared from Thread: ${Thread
            .currentThread().getName}")
      }
    }
  }

  def getTransaction(tokenAndRefCount: AgentBridge.TokenAndRefCount): Transaction =
    if (tokenAndRefCount != null && tokenAndRefCount.token != null) tokenAndRefCount.token.getTransaction.asInstanceOf[Transaction]
  else null

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

  def logTokenInfo(tokenAndRefCount: AgentBridge.TokenAndRefCount, msg: String): Unit = {
    if (AgentBridge.getAgent.getLogger.isLoggable(Level.FINEST)) {
      val tokenMsg = if (tokenAndRefCount != null && tokenAndRefCount.token != null) s"[${tokenAndRefCount.token}:${tokenAndRefCount.token.getTransaction}:${tokenAndRefCount.refCount.get}]"
      else "[Empty token]"
      AgentBridge.getAgent.getLogger.log(Level.FINEST,
        s"[${Thread.currentThread().getName}] ${tokenMsg}: [IOTick] token info ${msg}")
    }
  }

}
