package cats.effect

import cats.effect.unsafe.IORuntime
import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.weaver.{NewField, Weave, Weaver}
import com.nr.agent.instrumentation.Utils._

import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import scala.concurrent.ExecutionContext

@Weave(originalName = "cats.effect.IOFiber")
private final class IOFiber_Instrumentation[A](initLocalState: IOLocalState,
                               cb: OutcomeIO[A] => Unit,
                               startIO: IO[A],
                               startEC: ExecutionContext,
                               private[this] val runtime: IORuntime
                              ) {

  @NewField
  private var tokenAndRefCount: AgentBridge.TokenAndRefCount = getThreadTokenAndRefCount

  incrementTokenRefCount(this.tokenAndRefCount)
  logTokenInfo(tokenAndRefCount, "IOFiber token info set")
  private val IOEndFiber: IO.EndFiber.type = Weaver.callOriginal();

  def run(): Unit = {
    val fiberTokenEmpty = this.tokenAndRefCount == null
    try {
      if (!fiberTokenEmpty) setThreadTokenAndRefCount(tokenAndRefCount)
      Weaver.callOriginal
    } finally {
      val threadTokenAndRefCount = getThreadTokenAndRefCount
      if (fiberTokenEmpty && threadTokenAndRefCount != null) {
         this.tokenAndRefCount = threadTokenAndRefCount
        incrementTokenRefCount(this.tokenAndRefCount)
      }
      attemptExpireTokenRefCount(threadTokenAndRefCount)
    }
  }

  private def done(oc: OutcomeIO[A]): Unit = {
    Weaver.callOriginal()
    if (tokenAndRefCount != null) {
      decrementTokenRefCount(tokenAndRefCount)
      attemptExpireTokenRefCount(tokenAndRefCount)
    }
  }


  private[this] def scheduleFiber(ec: ExecutionContext)(fiber: IOFiber[_]): Unit = {
    clearTxn()
    Weaver.callOriginal()
  }

  private[this] def rescheduleFiber(ec: ExecutionContext)(fiber: IOFiber[_]): Unit = {
    clearTxn()
    Weaver.callOriginal()
  }

  private def runLoop(_cur0: IO[Any],
                      cancelationIterations: Int,
                      autoCedeIterations: Int): Unit = {
    val threadLocalTokenAndRefCount = AgentBridge.activeToken.get()
    if(this.tokenAndRefCount == null && threadLocalTokenAndRefCount != null) {
      this.tokenAndRefCount = threadLocalTokenAndRefCount
    }
    Weaver.callOriginal()
  }

  private  def clearTxn(): Unit = {
    AgentBridge.activeToken.remove()
    val txn = AgentBridge.getAgent.getTransaction(false)
    if(txn != null) {
      val txnCleared = txn.clearTransaction()
      if (AgentBridge.getAgent.getLogger.isLoggable(Level.FINEST))
        AgentBridge.getAgent.getLogger.log(Level.FINEST,
          s"[IOFiber] Txn ${txn.hashCode()} ${if (!txnCleared) "not"} " +
            s"cleared from Thread: ${Thread.currentThread().getName}")
    }
  }

}
