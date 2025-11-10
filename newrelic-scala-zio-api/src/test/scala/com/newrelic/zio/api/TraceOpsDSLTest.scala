package com.newrelic.zio.api

import com.newrelic.agent.introspec._
import com.newrelic.api.agent.Trace
import com.newrelic.zio.api.TraceOps._
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.junit.{After, Assert, Before, FixMethodOrder, Ignore, Test}
import zio.Exit.Success
import zio.clock.Clock

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import zio.{UIO, _}
import zio.duration.durationInt

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ZIOTraceOpsTests {

  val introspector: Introspector = InstrumentationTestRunner.getIntrospector

  def executorService(nThreads: Int) = {
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(nThreads))
  }

  val threadPoolOne: ExecutionContext = executorService(3)
  val threadPoolTwo: ExecutionContext = executorService(3)
  val threadPoolThree: ExecutionContext = executorService(3)

  @Before
  def setup() = {
    com.newrelic.agent.Transaction.clearTransaction
    introspector.clear()
  }
  @After
  def resetTxn() = {
    com.newrelic.agent.Transaction.clearTransaction
    introspector.clear()
  }

  @Test
  def asyncTraceProducesOneSegment(): Unit = {
    //When
    val txnBlock: UIO[Int] = txn {
      asyncTrace("getNumber")(UIO(1))
    }
    val result = Runtime.default.unsafeRunSync(txnBlock)
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
    //When
    val txnBlock: UIO[Int] = txn(
      asyncTrace("getNumber")(UIO(1))
        .map(traceFun("incrementNumber")(_ + 1))
        .flatMap(asyncTraceFun("flatMapIncrementNumber")(res => UIO(res + 1)))
    )
    val result = Runtime.default.unsafeRunSync(txnBlock)
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

    //When
    val txnBlock: UIO[Int] = txn {
      for {
        oneFibre <- asyncTrace("one")(
          UIO(1).on(threadPoolOne).fork)
        twoFibre <- asyncTrace("two")(
          UIO(2).on(threadPoolTwo).fork)
        three <- UIO(getThree)
        two <- twoFibre.join
        one <- oneFibre.join
      } yield one + two + three
    }
    val result = Runtime.default.unsafeRunSync(txnBlock)

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

    //When
    val txnBlock = txn(
      for {
        one <- asyncTrace("one")(UIO(1))
        _ <- asyncTrace("sleep")(ZIO.sleep(delayMillis.millis))
        two <- asyncTrace("two")(UIO(one + 1))
      } yield two
    )
    val result = Runtime.default.unsafeRunSync(txnBlock)
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
  def segmentCompletedIfIOErrors(): Unit = {

    //When
    val txnBlock: Task[Int] = txn(
      for {
        one <- asyncTrace("one")(IO(1))
        _ <- asyncTrace("boom")(IO.fail(new Throwable("Boom!")))
      } yield one
    )

    val result = Runtime.default.unsafeRunSync(txnBlock)

    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    //Then
    result.fold(
      failed => Assert.assertTrue("Result correct", failed.failed),
      _ => Assert.fail("Incorrect success result in test")
    )
    Assert.assertEquals("Transaction finished", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
    Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue("two segment exists", segments.exists(_.getName == s"Custom/boom"))
  }


  @Test
  def parallelTraverse(): Unit = {

    //When
    val txnBlock: UIO[Int] = txn(
      for {
        one <- UIO(trace("segment 1")(1))
        rest <- ZIO.foreachPar(List(2, 3, 4, 5))(i => asyncTrace(s"segment $i")(UIO(i)))
        sum <- asyncTrace("sum segments")(UIO(rest.sum + one))
      } yield sum
    )
    val result = Runtime.default.unsafeRunSync(txnBlock)
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

  /*
  Added in response to a bug discovered with ZIO instrumentation where back-to-back
  transactions with thread hops leaked into each other (resulting in 1 transaction instead of several).
  This was found to primarily affect transactions that started and ended on different threads (eg,
  because of a .join operation).

  This test is named to be evaluated last alphabetically due to side effects from the ZIO Runtime environment.
   */
  @Test
  @Ignore
  def z_multipleTransactionsWithThreadHopsDoNotBleed(): Unit = {
    val delayMillis = 500

    //When
    def txnBlock(i: Int): ZIO[Clock, Nothing, Int] = {
      txn(
        for {
          one <- asyncTrace(s"loop $i: one")(UIO(1))
          _ <- asyncTrace(s"loop $i: sleep")(ZIO.sleep(delayMillis.millis))//thread hop
          forkedFiber <- asyncTrace(s"loop $i: forked fiber")(UIO(5).fork) //thread hop
          five <- forkedFiber.join //thread hop
          eight <- asyncTrace(s"loop $i: eight")(UIO(one + five + 2))
        } yield eight + i
      )
    }
    val result = Runtime.default.unsafeRunSync(
      ZIO.loop(0)(i => i < 5, i => i + 1)(i => txnBlock(i))
    )

    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)

    Assert.assertEquals("Result correct", Success(List(8, 9, 10, 11, 12)), result)
    Assert.assertEquals("Correct number of transactions", 5, txnCount)
    Assert.assertEquals("Correct number of traces", 5, traces.size)
    List(0, 1, 2, 3, 4).foreach(i => {
      Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/loop $i: one"))
      Assert.assertTrue("sleep segment exists", segments.exists(_.getName == s"Custom/loop $i: sleep"))
      Assert.assertTrue("forked segment exists", segments.exists(_.getName == s"Custom/loop $i: forked fiber"))
      Assert.assertTrue("eight segment exists", segments.exists(_.getName == s"Custom/loop $i: eight"))
    })

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