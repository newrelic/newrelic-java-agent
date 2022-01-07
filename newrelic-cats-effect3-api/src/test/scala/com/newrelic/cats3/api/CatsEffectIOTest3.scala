package com.newrelic.cats3.api

import cats.effect.IO
import cats.syntax.parallel._
import TraceOps._
import com.newrelic.agent.introspec._
import org.junit.runner.RunWith
import org.junit.{After, Assert, Test}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.util.Try
import cats.effect.unsafe.implicits.global

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class CatsEffectIOTest3 {
  private def executorService(nThreads: Int): ExecutionContextExecutor = {
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(nThreads))
  }

  @After
  def resetTxn(): Unit = {
    com.newrelic.agent.Transaction.clearTransaction()
  }

  /**
    * asyncTraceProducesOneSegment: simple test to check a transaction with 1 segement
    * returning the correct result when run
    */
  @Test
  def asyncTraceProducesOneSegment(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When
    val txnBlock: IO[Int] = txn { implicit txnInfo =>
      asyncTrace("getNumber")(IO(1))
    }
    val result = txnBlock.evalOn(executorService(2)).unsafeRunTimed(2.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertEquals("result correct", Option(1), result)
    Assert.assertTrue("transaction finished", txnCount >= 1)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue("getFirstNumber segment exists", segments.exists(_.getName == s"Custom/getNumber"))
    introspector.clear()
  }

  /**
    * chainedSyncAndAsyncTraceSegmentsCaptured: test to check a transaction with 3 segements
    * created using `asyncTrace`, `traceFun` and `asyncTraceFun` together
    */
  @Test
  def chainedSyncAndAsyncTraceSegmentsCaptured(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When
    val txnBlock: IO[Int] = txn(implicit txnInfo =>
      asyncTrace("getNumber") {
        IO(1)
      }.map(traceFun("incrementNumber")(_ + 1))
       .flatMap(asyncTraceFun("flatMapIncrementNumber")(res => IO(res + 1)))
    )
    val result = txnBlock.evalOn(executorService(2)).unsafeRunTimed(2.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertEquals("Result correct", Option(3), result)
    Assert.assertTrue("Transaction finished", txnCount >= 1)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue("getNumber segment exists", segments.exists(_.getName == s"Custom/getNumber"))
    Assert.assertTrue("incrementNumber segment exists", segments.exists(_.getName == s"Custom/incrementNumber"))
    Assert.assertTrue("flatMapIncrementNumber segment exists", segments.exists(_.getName == s"Custom/flatMapIncrementNumber"))
    introspector.clear()
  }

  /**
    * forComprehensionSegments: test to check a transaction with 3 segments
    * created inside a for comprehension.
    */
  @Test
  def forComprehensionSegments(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When
    val txnBlock: IO[Int] = txn(implicit txnInfo =>
      for {
        one <- asyncTrace("one")(IO(1))
        two <- asyncTrace("two")(IO(one + 1))
        three <- asyncTrace("three")(IO(two + 1))
      } yield three
    )
    val result = txnBlock.evalOn(executorService(2)).unsafeRunTimed(2.seconds)
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
    introspector.clear()
  }

  /**
    * forComprehensionSegmentsSeparateExecutors: test to check a transaction with 3 segments
    * created inside a for comprehension. Each segement is executed on a separate thread pool
    * using cats-effect `evalOn`
    */
  @Test
  def forComprehensionSegmentsSeparateExecutors(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val txnBlock: IO[Int] = txn(implicit txnInfo =>
      for {
        one <- asyncTrace("one")(IO(1).evalOn(executorService(3)))
        two <- asyncTrace("two")(IO(one + 1).evalOn(executorService(5)))
        three <- asyncTrace("three")(IO(two + 1).evalOn(executorService(10)))
      } yield three)
    //When
    val result = txnBlock.evalOn(executorService(2)).unsafeRunTimed(2.seconds)
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    val segmentNames = segments.map(_.getName).toList
    //Then
    Assert.assertEquals("Result correct", Some(3), result)
    Assert.assertTrue("Transaction finished", txnCount >= 1)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue(s"one segment exists $segmentNames", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue(s"two segment exists $segmentNames", segments.exists(_.getName == s"Custom/two"))
    Assert.assertTrue(s"three segment exists $segmentNames", segments.exists(_.getName == s"Custom/three"))
    introspector.clear()
  }

  /**
    * sequentialAsyncTraceSegmentTimeCaptured: test to check a transaction with 3 segments
    * created inside a for comprehension.
    * The 2nd segment "sleep" is scheduled to complete after 1500 milliseconds, the test asserts that
    * the segment takes at least 1500 milliseconds to complete
    */
  @Test
  def sequentialAsyncTraceSegmentTimeCaptured(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val delayMillis = 1500
    //When
    val txnBlock: IO[Int] = txn(implicit txnInfo =>
      for {
        one <- asyncTrace("one")(IO(1))
        _ <- asyncTrace("sleep")(IO.sleep(delayMillis.millis).evalOn(executorService(3)))
        two <- asyncTrace("two")(IO(one + 1))
      } yield two
    )
    val result = txnBlock.evalOn(executorService(2)).unsafeRunTimed(2.seconds)
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
    introspector.clear()
  }


  /**
    * segmentCompletedIfIOErrors: test to check a transaction with 2 segments
    * created inside a for comprehension.
    * The 2nd segment "sleep" is creates an error causing the block to also yield an
    * error when run.
    * The test asserts that both successfull and error segments are captured
    */
  @Test
  def segmentCompletedIfIOErrors(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    //When
    val txnBlock: IO[Int] = txn(implicit txnInfo =>
      for {
        one <- asyncTrace("one")(IO(1).evalOn(executorService(3)))
        _ <- asyncTrace("boom")(IO.raiseError(new Throwable("Boom!")))
      } yield one
    )
    val result = Try(txnBlock.evalOn(executorService(2)).unsafeRunSync())
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    //Then
    Assert.assertTrue("Result correct", result.isFailure)
    Assert.assertTrue("Transaction finished", txnCount >= 1)
    Assert.assertTrue("Trace present", traces.nonEmpty)
    Assert.assertTrue("one segment exists", segments.exists(_.getName == s"Custom/one"))
    Assert.assertTrue("two segment exists", segments.exists(_.getName == s"Custom/boom"))
    introspector.clear()
  }

  /**
    * parallelTraverse: test to check a transaction with 6 segments
    * created inside a for comprehension.
    * Segments 2-5 are created in parallel, the final segment is executed after
    * the others complete summing the results of Ints returned from each segment
    * The test asserts that both sequential and parrallel segments are captured
    */
  @Test
  def parallelTraverse(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val txnBlock: IO[Int] = txn(implicit txnInfo =>
      for {
        one <- IO(trace("segment 1")(1))
        rest <- List(2, 3, 4, 5).parTraverse(i => asyncTrace(s"segment $i")(IO(i)))
        sum <- asyncTrace("sum segments")(IO(rest.sum + one))
      } yield sum
    )

    val result = txnBlock.evalOn(executorService(2)).unsafeRunTimed(2.seconds)

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
    introspector.clear()
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
