package com.newrelic.monix.api

import monix.execution.Scheduler.Implicits.global
import TraceOps._
import com.newrelic.agent.introspec._
import org.junit.runner.RunWith
import org.junit.{After, Assert, Test}

import scala.concurrent.Await
import scala.jdk.CollectionConverters._
import monix.eval.Task
import monix.execution.Scheduler

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.util.Try

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class MonixTaskTest {
  @After
  def resetTxn(): Unit = {
    com.newrelic.agent.Transaction.clearTransaction()
  }

  @Test
  def asyncTraceProducesOneSegment(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When
    val txnBlock: Task[Int] = txn {
      asyncTrace("getNumber")(Task(1))
    }
    val result = txnBlock.runToFuture
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertEquals("result correct", 1, Await.result(result, 1.seconds))
    Assert.assertTrue("transaction finished", txnCount >= 1)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue("getFirstNumber segment exists", segments.exists(_.getName == s"Custom/getNumber"))
  }

  @Test
  def chainedSyncAndAsyncTraceSegmentsCaptured(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When
    val txnBlock: Task[Int] = txn(
      asyncTrace("getNumber") {
        Task(1)
      }
        .map(traceFun("incrementNumber")(_ + 1))
        .flatMap(asyncTraceFun("flatMapIncrementNumber")(res => Task(res + 1)))
    )
    val result = txnBlock.runToFuture
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertEquals("Result correct", 3, Await.result(result, 1.seconds))
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue("getNumber segment exists", segments.exists(_.getName == s"Custom/getNumber"))
    Assert.assertTrue("incrementNumber segment exists", segments.exists(_.getName == s"Custom/incrementNumber"))
    Assert.assertTrue("flatMapIncrementNumber segment exists", segments.exists(_.getName == s"Custom/flatMapIncrementNumber"))
  }

  @Test
  def forComprehensionSegments(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When
    val txnBlock: Task[Int] = txn(
      for {
        one <- asyncTrace("one")(Task(1))
        two <- asyncTrace("two")(Task(one + 1))
        three <- asyncTrace("three")(Task(two + 1))
      } yield three
    )
    val result = txnBlock.runToFuture
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertEquals("Result correct", 3, Await.result(result, 1.seconds))
    Assert.assertTrue("Transaction finished", txnCount >= 1)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue("two segment exists", segments.exists(_.getName == s"Custom/two"))
    Assert.assertTrue("three segment exists", segments.exists(_.getName == s"Custom/three"))
  }

  @Test
  def segmentCompletedIfIOErrors(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    lazy val executorService = scala.concurrent.ExecutionContext.Implicits.global
    lazy val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
    lazy val scheduler = Scheduler(scheduledExecutor, executorService)
    //When
    val txnBlock: Task[Int] = txn(
      for {
        one <- asyncTrace("one")(Task(1).executeOn(scheduler))
        _ <- asyncTrace("boom")(Task.raiseError(new Throwable("Boom!")))
      } yield one
    )
    val result = Try(txnBlock.runToFuture)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertTrue("Result correct", result.failed.isFailure)
    Assert.assertTrue("Transaction finished", txnCount >= 1)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue("two segment exists", segments.exists(_.getName == s"Custom/boom"))
  }

  private def getTraces(introspector: Introspector): Iterable[TransactionTrace] =
    introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)

  private def getSegments(traces: Iterable[TransactionTrace]): Iterable[TraceSegment] =
    traces.flatMap(trace => this.getSegments(trace.getInitialTraceSegment))

  private def getSegments(segment: TraceSegment): List[TraceSegment] = {
    val childSegments = segment.getChildren.asScala.flatMap(childSegment => getSegments(childSegment)).toList
    segment :: childSegments
  }
}