package com.nr.agent.instrumentation.scala.baseline

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector}
import com.newrelic.api.agent.Trace
import com.nr.agent.instrumentation.scala.TracerSegmentUtils.{getSegments, getTraces}
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class UnmarkedSegmentNestedAsynchronousSingleThreadTest {

  implicit val singleThread: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  @Test
  def oneNestedTransaction(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getOneResult

    //Then
    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertEquals("Result", 1, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 1, segments.size)
  }

  @Test
  def twoNestedTransactions(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getTwoResults

    //Then
    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertEquals("Result", 3, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 1, segments.size)
  }

  @Test
  def threeNestedTransactions(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getThreeResults

    //Then
    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertEquals("Result", 6, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 1, segments.size)
  }

  @Trace(dispatcher = true)
  def getOneResult: Future[Int] = Future.reduceLeft(Seq(getFirstNumber))(_ + _)

  @Trace(dispatcher = true)
  def getTwoResults: Future[Int] = Future.reduceLeft(Seq(getFirstNumber, getSecondNumber))(_ + _)

  @Trace(dispatcher = true)
  def getThreeResults: Future[Int] = Future.reduceLeft(Seq(getFirstNumber, getSecondNumber, getThirdNumber))(_ + _)

  def getFirstNumber: Future[Int] = Future{
    1
  }

  def getSecondNumber: Future[Int] = Future{
    2
  }

  def getThirdNumber: Future[Int] = Future{
    3
  }
}
