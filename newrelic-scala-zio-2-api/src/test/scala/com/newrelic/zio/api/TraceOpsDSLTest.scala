package com.newrelic.zio.api

import com.newrelic.agent.introspec._
import com.newrelic.api.agent.Trace
import com.newrelic.zio.api.TraceOps._
import org.junit.runner.RunWith
import org.junit.{After, Assert, Test}
import zio.Exit.Success
import zio.{UIO, _}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class ZIOTraceOpsTests {

  def executorService(nThreads: Int) = {
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(nThreads))
  }

  val threadPoolOne: ExecutionContext = executorService(3)
  val threadPoolTwo: ExecutionContext = executorService(3)
  val threadPoolThree: ExecutionContext = executorService(3)

  @After
  def resetTxn() = {
    com.newrelic.agent.Transaction.clearTransaction
  }

  @Test
  def asyncTraceProducesOneSegment(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val txnBlock: UIO[Int] = txn {
      asyncTrace("getNumber")(ZIO.succeed(1))
    }
    val result = Unsafe.unsafe(implicit unsafe => Runtime.default.unsafe.run(txnBlock))
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    //Then
    Assert.assertEquals("result correct", Success(1), result)
    Assert.assertEquals("transaction finished", 1, txnCount)
    Assert.assertEquals("trace present", 1, traces.size)
    Assert.assertTrue("getFirstNumber segment exists",
      segments.exists(_.getName == s"Custom/getNumber")
    )
  }

  @Test
  def chainedSyncAndAsyncTraceSegmentsCaptured(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val txnBlock: UIO[Int] = txn(
      asyncTrace("getNumber")(ZIO.succeed(1))
        .map(traceFun("incrementNumber")(_ + 1))
        .flatMap(asyncTraceFun("flatMapIncrementNumber")(res => ZIO.succeed(res + 1)))
    )
    val result = Unsafe.unsafe(implicit unsafe => Runtime.default.unsafe.run(txnBlock))
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    //Then
    Assert.assertEquals("Result correct", Success(3), result)
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
    Assert.assertTrue("getNumber segment exists", segments.exists(_.getName == s"Custom/getNumber"))
    Assert.assertTrue("incrementNumber segment exists", segments.exists(_.getName == s"Custom/incrementNumber"))
    Assert.assertTrue("flatMapIncrementNumber segment exists", segments.exists(_.getName == s"Custom/flatMapIncrementNumber"))
  }

  @Test
  def asyncForComprehensionSegments: Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val txnBlock: UIO[Int] = txn {
      for {
        oneFibre <- asyncTrace("one")(
          ZIO.succeed(1).onExecutionContext(threadPoolOne).fork)
        twoFibre <- asyncTrace("two")(
          ZIO.succeed(2).onExecutionContext(threadPoolTwo).fork)
        three <- ZIO.succeed(getThree)
        two <- twoFibre.join
        one <- oneFibre.join
      } yield one + two + three
    }
    val result = Unsafe.unsafe(implicit unsafe => Runtime.default.unsafe.run(txnBlock))

    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    Assert.assertEquals("Result correct", Success(6), result)
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
    Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue("two segment exists", segments.exists(_.getName == s"Custom/two"))
    Assert.assertTrue("getThree segment exists", segments.exists(_.getName.endsWith("getThree")))
  }


  @Test
  def sequentialAsyncTraceSegmentTimeCaptured(): Unit = {
    val delayMillis = 1500
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val txnBlock = txn(
      for {
        one <- asyncTrace("one")(ZIO.succeed(1))
        _ <- asyncTrace("sleep")(ZIO.sleep(delayMillis.millis))
        two <- asyncTrace("two")(ZIO.succeed(one + 1))
      } yield two
    )
    val result = Unsafe.unsafe(implicit unsafe => Runtime.default.unsafe.run(txnBlock))
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    //Then
    Assert.assertEquals("Result correct", Success(2), result)
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)

    Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue("two segment exists", segments.exists(_.getName == s"Custom/two"))
    assertSegmentExistsAndTimeInRange("sleep", segments, delayMillis)

  }

  @Test
  def segmentCompletedIfZIOErrors(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val txnBlock: Task[Int] = txn(
      for {
        one <- asyncTrace("one")(ZIO.succeed(1))
        _ <- asyncTrace("boom")(ZIO.fail(new Throwable("Boom!")))
      } yield one
    )

    val result = Unsafe.unsafe(implicit unsafe => Runtime.default.unsafe.run(txnBlock))

    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    //Then
    result.foldExit(
      failed => Assert.assertTrue("Result correct", failed.isFailure),
      _ => Assert.fail("Incorrect success result in test")
    )
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
    Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue("two segment exists", segments.exists(_.getName == s"Custom/boom"))
  }


  @Test
  def parallelTraverse(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val txnBlock: UIO[Int] = txn(
      for {
        one <- ZIO.succeed(trace("segment 1")(1))
        rest <- ZIO.foreachPar(List(2, 3, 4, 5))(i => asyncTrace(s"segment $i")(ZIO.succeed(i)))
        sum <- asyncTrace("sum segments")(ZIO.succeed(rest.sum + one))
      } yield sum
    )
    val result = Unsafe.unsafe(implicit unsafe => Runtime.default.unsafe.run(txnBlock))
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    //Then
    Assert.assertEquals("Result correct", Success(15), result)
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
    List(1, 2, 3, 4, 5).foreach(i =>
      Assert.assertTrue(s"$i segment exists", segments.exists(_.getName == s"Custom/segment $i"))
    )
    Assert.assertTrue(s"sum segments exists", segments.exists(_.getName == s"Custom/sum segments"))
  }

  @Trace(async = true)
  private def getThree = 3

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
