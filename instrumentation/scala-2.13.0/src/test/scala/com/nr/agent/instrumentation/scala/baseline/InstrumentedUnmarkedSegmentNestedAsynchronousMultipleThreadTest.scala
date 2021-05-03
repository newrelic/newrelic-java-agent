package com.nr.agent.instrumentation.scala.baseline

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector}
import com.newrelic.api.agent.Trace
import com.nr.agent.instrumentation.scala.TracerSegmentUtils.{getSegments, getTraces}
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

import java.util.concurrent.{Executors, TimeUnit}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("scala.concurrent.impl"), configName = "scala_futures_as_segments.yml")
class InstrumentedUnmarkedSegmentNestedAsynchronousMultipleThreadTest {

  val threadPoolOne: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
  val threadPoolTwo: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
  val threadPoolThree: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  @Test
  def oneNestedTransaction(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getOneResult

    //Then
    introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10))

    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertEquals("Result", 1, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 3, segments.size)
  }

  @Test
  def twoNestedTransactions(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getTwoResults

    //Then
    introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10))

    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertEquals("Result", 3, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 5, segments.size)
  }

  @Test
  def threeNestedTransactions(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getThreeResults

    //Then
    introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10))

    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertEquals("Result", 6, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 7, segments.size)
  }

  @Test
  def deepNestedTransactions(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getThreeNestedResults

    //Then
    introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10))

    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertEquals("Result", 6, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 15, segments.size)
  }

  @Trace(dispatcher = true)
  def getOneResult: Future[Int] = Future.reduceLeft(Seq(getFirstNumber))(_ + _)(threadPoolOne)

  @Trace(dispatcher = true)
  def getTwoResults: Future[Int] = Future.reduceLeft(Seq(getFirstNumber, getSecondNumber))(_ + _)(threadPoolTwo)

  @Trace(dispatcher = true)
  def getThreeResults: Future[Int] = Future.reduceLeft(Seq(getFirstNumber, getSecondNumber, getThirdNumber))(_ + _)(threadPoolThree)

  @Trace(dispatcher = true)
  def getThreeNestedResults: Future[Int] = Future.reduceLeft(Seq(getFirstNumber, getNestedSecondNumber, getNestedThirdNumber))(_ + _)(threadPoolThree)

  def getFirstNumber: Future[Int] = Future{
    1
  }(threadPoolOne)

  def getSecondNumber: Future[Int] = Future{
    2
  }(threadPoolTwo)

  def getNestedSecondNumber: Future[Int] = {
    Future.reduceLeft(Seq(getFirstNumber, getFirstNumber))(_ + _)(threadPoolThree)
  }

  def getThirdNumber: Future[Int] = Future{
    3
  }(threadPoolThree)

  def getNestedThirdNumber: Future[Int] = {
    implicit val ec: ExecutionContext = threadPoolOne
    getFirstNumber.flatMap(x => getFirstNumber.flatMap(y => getFirstNumber.map(z => x + y +z)))
  }
}
