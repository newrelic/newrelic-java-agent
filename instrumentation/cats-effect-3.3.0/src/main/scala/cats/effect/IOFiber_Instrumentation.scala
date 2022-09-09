package cats.effect

import cats.effect.internals.Utils._
import cats.effect.tracing.RingBuffer
import cats.effect.unsafe.IORuntime
import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.weaver.{NewField, Weave, Weaver}

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

  //possibly wrap startEc

  @NewField
  private val tokenAndRefCount: AtomicReference[AgentBridge.TokenAndRefCount] =
    new AtomicReference(getThreadTokenAndRefCount(fiberRef))

  incrementTokenRefCount(this.tokenAndRefCount.get())
  logTokenInfo(tokenAndRefCount.get(), fiberRef, "IOFiber token info set")
  private val IOEndFiber: IO.EndFiber.type = Weaver.callOriginal();

  private def fiberRef: IOFiber[A] = this.asInstanceOf[IOFiber[A]]
  def run(): Unit = {
    val fiberTokenEmpty = this.tokenAndRefCount.get() == null
    try {
      if (!fiberTokenEmpty) setThreadTokenAndRefCount(tokenAndRefCount, fiberRef)
      Weaver.callOriginal
    } finally {
      val threadTokenAndRefCount = getThreadTokenAndRefCount(fiberRef)
      if (fiberTokenEmpty && threadTokenAndRefCount != null) {
        if (threadTokenAndRefCount != null) {
          this.tokenAndRefCount.set(threadTokenAndRefCount)
        }
        incrementTokenRefCount(this.tokenAndRefCount.get())
      }
      attemptExpireTokenRefCount(threadTokenAndRefCount, fiberRef)
    }
  }

  private def done(oc: OutcomeIO[A]): Unit = {
    Weaver.callOriginal()
    if (tokenAndRefCount != null) {
      decrementTokenRefCount(tokenAndRefCount.get())
      attemptExpireTokenRefCount(tokenAndRefCount.get(), fiberRef)
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
    if(this.tokenAndRefCount.get() == null && threadLocalTokenAndRefCount != null)
      this.tokenAndRefCount.set(threadLocalTokenAndRefCount)

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
