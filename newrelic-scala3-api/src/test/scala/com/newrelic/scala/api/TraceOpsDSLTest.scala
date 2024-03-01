package com.newrelic.scala.api

import java.util.concurrent.Executors

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector, TraceSegment, TransactionTrace}
import com.newrelic.scala.api.TraceOps._
import org.junit.runner.RunWith
import org.junit.{After, Assert, Test}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

import scala.jdk.CollectionConverters._

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("scala.concurrent.impl."))
class TraceOpsDSLTest {

  val threadPoolThree: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(3))

  @After
  def resetTxn() = {
    com.newrelic.agent.Transaction.clearTransaction
  }

  @Test
  def asyncTraceProducesOneSegment(): Unit = {
    implicit val ec: ExecutionContext = threadPoolThree

    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val txnBlock: Future[Int] = txn {
      asyncTrace("getNumber")(Future(1))
    }
    val result = Await.result(txnBlock, 2.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    //Then
    Assert.assertEquals("result correct", 1, result)
    Assert.assertEquals("transaction finished", 1, txnCount)
    Assert.assertEquals("trace present", 1, traces.size)
    Assert.assertTrue("getFirstNumber segment exists", segments.exists(_.getName == s"Custom/getNumber"))
  }

  @Test
  def syncTraceProducesOneSegment(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val txnBlock: Int = txn {
      trace("getNumber")(1)
    }

    val txnCount = introspector.getFinishedTransactionCount(2000)
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    //Then
    Assert.assertEquals("result correct", 1, txnBlock)
    Assert.assertEquals("transaction finished", 1, txnCount)
    Assert.assertEquals("trace present", 1, traces.size)
    Assert.assertTrue("getFirstNumber segment exists", segments.exists(_.getName == s"Custom/getNumber"))

  }

  @Test
  def chainedSyncTraceSegmentsCaptured(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When

    val txnBlock: Option[Int] = txn(
      Option(trace("getNumber")(1))
        .map(traceFun("incrementNumber")(_ + 1))
        .flatMap(traceFun("flatMapIncrementNumber")(res => Option(res + 1)))
        .filter(traceFun("filterNumber")(_ % 2 == 1))
    )
    val txnCount = introspector.getFinishedTransactionCount(2000)
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    Assert.assertEquals("Result correct", Some(3), txnBlock)
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
    Assert.assertTrue("getNumber segment exists", segments.exists(_.getName == s"Custom/getNumber"))
    Assert.assertTrue("incrementNumber segment exists", segments.exists(_.getName == s"Custom/incrementNumber"))
    Assert.assertTrue("flatMapIncrementNumber segment exists", segments.exists(_.getName == s"Custom/flatMapIncrementNumber"))
    Assert.assertTrue("filterNumber segment exists", segments.exists(_.getName == s"Custom/filterNumber"))
  }

  @Test
  def chainedSyncAndAsyncTraceSegmentsCaptured(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When
    implicit val ec: ExecutionContext = threadPoolThree

    val txnBlock: Future[Int] = txn(
      asyncTrace("getNumber")(Future(1))
        .map(traceFun("incrementNumber")(_ + 1))
        .flatMap(asyncTraceFun("flatMapIncrementNumber")(res => Future(res + 1)))
        .filter(traceFun("filterNumber")(_ % 2 == 1))
    )
    val result = Await.result(txnBlock, 2.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    Assert.assertEquals("Result correct", 3, result)
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
    Assert.assertTrue("getNumber segment exists", segments.exists(_.getName == s"Custom/getNumber"))
    Assert.assertTrue("incrementNumber segment exists", segments.exists(_.getName == s"Custom/incrementNumber"))
    Assert.assertTrue("flatMapIncrementNumber segment exists", segments.exists(_.getName == s"Custom/flatMapIncrementNumber"))
    Assert.assertTrue("filterNumber segment exists", segments.exists(_.getName == s"Custom/filterNumber"))
  }

  @Test
  def syncForComprehensionSegments(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val txnBlock: Option[Int] = txn(
      for {
        one <- Option(trace("one")(1))
        two <- Option(trace("two")(one + 1))
        three <- Option(trace("three")(two + 1))
      } yield three
    )
    val txnCount = introspector.getFinishedTransactionCount(2000)
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    Assert.assertEquals("Result correct", Some(3), txnBlock)
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
    Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue("two segment exists", segments.exists(_.getName == s"Custom/two"))
    Assert.assertTrue("three segment exists", segments.exists(_.getName == s"Custom/three"))
  }


  @Test
  def asyncForComprehensionSegments(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    implicit val ec: ExecutionContext = threadPoolThree
    val txnBlock: Future[Int] = txn(
      for {
        one <- asyncTrace("one")(Future(1))
        two <- Future(trace("two")(one + 1))
        three <- asyncTrace("three")(Future(two + 1))
      } yield three
    )

    val result = Await.result(txnBlock, 2.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    Assert.assertEquals("Result correct", 3, result)
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
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
    implicit val ec: ExecutionContext = threadPoolThree

    //create 3 numbers in parallel then sum the result
    val txnBlock: Future[Int] = txn(
      asyncTrace("root")(
        for {
          one <- asyncTrace("one")(scheduleFuture(delayMillis.millis)(1))
          two <- asyncTrace("two")(scheduleFuture(delayMillis.millis)(one + 1))
          three <- asyncTrace("three")(scheduleFuture(delayMillis.millis)(two + 1))
        } yield three)
    )
    val result = Await.result(txnBlock, 6.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    Assert.assertEquals("Result correct", 3, result)
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)

    //ensure root segment exists and time take is at least the time taken for other 3 segments executed sequentially
    assertSegmentExistsAndTimeInRange("root", segments, delayMillis * 3)
    assertSegmentExistsAndTimeInRange("one", segments, delayMillis)
    assertSegmentExistsAndTimeInRange("two", segments, delayMillis)
    assertSegmentExistsAndTimeInRange("three", segments, delayMillis)

  }

  @Test
  def parrallelAsyncTraceSegmentTimeCaptured(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    val delayMillis = 1500
    //When
    implicit val ec: ExecutionContext = threadPoolThree

    //create 3 numbers in parallel then sum the result
    val txnBlock: Future[Int] = txn(
      asyncTrace("root")(Future.sequence(
        List(
          asyncTrace("one")(scheduleFuture(delayMillis.millis)(1)),
          asyncTrace("two")(scheduleFuture(delayMillis.millis)(2)),
          asyncTrace("three")(scheduleFuture(delayMillis.millis)(3)),
        )
      ).map(_.sum))
    )
    val result = Await.result(txnBlock, 2.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    Assert.assertEquals("Result correct", 6, result)
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)

    //ensure root segment exists and time take is less than the 3 segments executed sequentially
    assertSegmentExistsAndTimeInRange("root", segments, delayMillis, Some(delayMillis * 3 - 100))
    assertSegmentExistsAndTimeInRange("one", segments, delayMillis)
    assertSegmentExistsAndTimeInRange("two", segments, delayMillis)
    assertSegmentExistsAndTimeInRange("three", segments, delayMillis)

  }

  private def scheduleFuture[T](delay: FiniteDuration)(body: => T): Future[T] = {
    implicit val ec: ExecutionContext = threadPoolThree

    Future {
      Thread.sleep(delay.toMillis)
      body
    }
  }

  private def getTraces(introspector: Introspector): Iterable[TransactionTrace] =
    introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)

  private def getSegments(traces: Iterable[TransactionTrace]): Iterable[TraceSegment] =
    traces.flatMap(trace => this.getSegments(trace.getInitialTraceSegment))

  private def getSegments(segment: TraceSegment): List[TraceSegment] = {
    val childSegments = segment.getChildren.asScala.flatMap(childSegment => getSegments(childSegment)).toList
    segment :: childSegments
  }

  private def assertSegmentExistsAndTimeInRange(segmentName: String, segments: Iterable[TraceSegment], minTime: Long, optMaxTime: Option[Long] = None) = {
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

