package com.nr.agent.instrumentation.scala.baseline

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector}
import com.newrelic.api.agent.Trace
import com.nr.agent.instrumentation.scala.TracerSegmentUtils.{finishedTransactionCollection, getSegments, getTraces}
import org.junit.runner.RunWith
import org.junit.{Assert, Ignore, Test}

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

@Ignore
@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("scala.concurrent.impl"))
class InstrumentedSegmentNestedAsynchronousSingleThreadTest {

  implicit val singleThread: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  @Test
  def oneNestedTransaction(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getOneResult

    //Then
    Await.result(finishedTransactionCollection(), 10.seconds)

    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertEquals("Result", 1, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 0, traces.size)
    Assert.assertEquals("Segments", 0, segments.size)
  }

  @Test
  def twoNestedTransactions(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getTwoResults

    //Then
    Await.result(finishedTransactionCollection(), 10.seconds)

    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertEquals("Result", 3, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 0, traces.size)
    Assert.assertEquals("Segments", 0, segments.size)
  }

  @Test
  def threeNestedTransactions(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getThreeResults

    //Then
    Await.result(finishedTransactionCollection(), 10.seconds)

    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertEquals("Result", 6, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 0, traces.size)
    Assert.assertEquals("Segments", 0, segments.size)
  }

  @Trace(dispatcher = true)
  def getOneResult: Future[Int] = Future.reduceLeft(Seq(getFirstNumber))(_ + _)

  @Trace(dispatcher = true)
  def getTwoResults: Future[Int] = Future.reduceLeft(Seq(getFirstNumber, getSecondNumber))(_ + _)

  @Trace(dispatcher = true)
  def getThreeResults: Future[Int] = Future.reduceLeft(Seq(getFirstNumber, getSecondNumber, getThirdNumber))(_ + _)

  @Trace
  def getFirstNumber: Future[Int] = Future{
    println(s"${Thread.currentThread.getName}: getFirstNumber")
    1
  }

  @Trace
  def getSecondNumber: Future[Int] = Future{
    println(s"${Thread.currentThread.getName}: getSecondNumber")
    2
  }

  @Trace
  def getThirdNumber: Future[Int] = Future{
    println(s"${Thread.currentThread.getName}: getThirdNumber")
    3
  }
}
