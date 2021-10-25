package com.newrelic.cats3.api

import cats.effect.IO
import cats.syntax.parallel._
import TraceOps._
import com.newrelic.agent.introspec._
import org.junit.runner.RunWith
import org.junit.{After, Assert, Test}
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.util.Try
import cats.effect.unsafe.implicits.global

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class CatsEffectIOTest3 {
  private def executorService(nThreads: Int) = {
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(nThreads))
  }

  def threadPoolOne: ExecutionContext = executorService(3)

  def threadPoolTwo: ExecutionContext = executorService(3)

  def threadPoolThree: ExecutionContext = executorService(3)

  @After
  def resetTxn(): Unit = {
    com.newrelic.agent.Transaction.clearTransaction()
  }

  @Test
  def asyncTraceProducesOneSegment(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When
    val txnBlock: IO[Int] = txn {
      asyncTrace("getNumber")(IO(1))
    }
    val result = txnBlock.unsafeRunTimed(2.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertEquals("result correct", Option(1), result)
    Assert.assertTrue("transaction finished", txnCount >= 1)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue("getFirstNumber segment exists", segments.exists(_.getName == s"Custom/getNumber"))
  }

  @Test
  def chainedSyncAndAsyncTraceSegmentsCaptured(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When
    val txnBlock: IO[Int] = txn(
      asyncTrace("getNumber") {
        IO(1)
      }
        .map(traceFun("incrementNumber")(_ + 1))
        .flatMap(asyncTraceFun("flatMapIncrementNumber")(res => IO(res + 1)))
    )
    val result = txnBlock.unsafeRunTimed(2.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertEquals("Result correct", Option(3), result)
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
    val txnBlock: IO[Int] = txn(
      for {
        one <- asyncTrace("one")(IO(1))
        two <- asyncTrace("two")(IO(one + 1))
        three <- asyncTrace("three")(IO(two + 1))
      } yield three
    )
    val result = txnBlock.unsafeRunTimed(2.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertEquals("Result correct", Some(3), result)
    Assert.assertTrue("Transaction finished", txnCount >= 1)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue("two segment exists", segments.exists(_.getName == s"Custom/two"))
    Assert.assertTrue("three segment exists", segments.exists(_.getName == s"Custom/three"))
  }

  @Test
  def sequentialAsyncTraceSegmentTimeCaptured(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val delayMillis = 1500
    //When
    val txnBlock: IO[Int] = txn(
      for {
        one <- asyncTrace("one")(IO(1))
        _ <- asyncTrace("sleep")(IO.sleep(delayMillis.millis))
        two <- asyncTrace("two")(IO(one + 1))
      } yield two
    )
    val result = txnBlock.unsafeRunTimed(2.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertEquals("Result correct", Option(2), result)
    Assert.assertTrue("Transaction finished", txnCount >= 1)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue("two segment exists", segments.exists(_.getName == s"Custom/two"))
    assertSegmentExistsAndTimeInRange("sleep", segments, delayMillis)
  }


  @Test
  def segmentCompletedIfIOErrors(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When
    val txnBlock: IO[Int] = txn(
      for {
        one <- asyncTrace("one")(IO(1).evalOn(threadPoolOne))
        _ <- asyncTrace("boom")(IO.raiseError(new Throwable("Boom!")))
      } yield one
    )
    val result = Try(txnBlock.unsafeRunSync())
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertTrue("Result correct", result.isFailure)
    Assert.assertTrue("Transaction finished", txnCount >= 1)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue("two segment exists", segments.exists(_.getName == s"Custom/boom"))
  }

  @Test
  def parallelTraverse(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val txnBlock: IO[Int] = txn(
      for {
        one <- IO(trace("segment 1")(1))
        rest <- List(2, 3, 4, 5).parTraverse(i => asyncTrace(s"segment $i")(IO(i)))
        sum <- asyncTrace("sum segments")(IO(rest.sum + one))
      } yield sum
    )

    val result = txnBlock.unsafeRunTimed(2.seconds)

      val txnCount = introspector.getFinishedTransactionCount()
      val traces = getTraces(introspector)
      val segments = getSegments(traces)

      Assert.assertEquals("Result correct", Some(15), result)
      Assert.assertTrue("Transaction finished", txnCount >= 1)
      Assert.assertTrue("Trace present", traces.nonEmpty)
      List(1, 2, 3, 4, 5).foreach(i =>
        Assert.assertTrue(s"$i segment exists: ${segments.map(_.getName)}", segments.exists(_.getName == s"Custom/segment $i"))
      )
      Assert.assertTrue(s"sum segments exists", segments.exists(_.getName == s"Custom/sum segments"))
  }

  private def getTraces(introspector: Introspector): Iterable[TransactionTrace] =
    introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)

  private def getSegments(traces: Iterable[TransactionTrace]): Iterable[TraceSegment] =
    traces.flatMap(trace => this.getSegments(trace.getInitialTraceSegment))

  private def getSegments(segment: TraceSegment): List[TraceSegment] = {
    val childSegments = segment.getChildren.asScala.flatMap(childSegment => getSegments(childSegment)).toList
    segment :: childSegments
  }

  private def assertSegmentExistsAndTimeInRange(segmentName: String,
                                                segments   : Iterable[TraceSegment],
                                                minTime    : Long,
                                                optMaxTime : Option[Long] = None): Unit = {
    val optDelayedSegment: Option[TraceSegment] = segments.find(_.getName == s"Custom/$segmentName")
    Assert.assertTrue(s"scheduled $segmentName segment exists", optDelayedSegment.isDefined)
    optDelayedSegment.foreach(delayedSegmentTime => {
      Assert.assertTrue(s"delayedSegmentTime $segmentName less than minimum expected time", delayedSegmentTime.getRelativeEndTime >= minTime)
      optMaxTime.foreach(maxTime =>
        Assert.assertTrue(s"delayedSegmentTime $segmentName greater than maximum expected time", delayedSegmentTime.getRelativeEndTime <= maxTime)
      )
    })
  }
}
