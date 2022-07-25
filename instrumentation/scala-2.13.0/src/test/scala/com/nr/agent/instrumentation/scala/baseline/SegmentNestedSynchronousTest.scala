package com.nr.agent.instrumentation.scala.baseline

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector}
import com.newrelic.api.agent.Trace
import com.nr.agent.instrumentation.scala.TracerSegmentUtils.{getSegments, getTraces}
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class SegmentNestedSynchronousTest {

  @Test
  def oneNestedTransaction(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val result = getOneResult

    //Then
    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertEquals("Result", 1, result)
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 2, segments.size)
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

    Assert.assertEquals("Result", 3, result)
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 3, segments.size)
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

    Assert.assertEquals("Result", 6, result)
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 4, segments.size)
  }

  @Trace(dispatcher = true)
  def getOneResult: Int = getFirstNumber

  @Trace(dispatcher = true)
  def getTwoResults: Int = getFirstNumber + getSecondNumber

  @Trace(dispatcher = true)
  def getThreeResults: Int = getFirstNumber + getSecondNumber + getThirdNumber

  @Trace
  def getFirstNumber: Int = {
    1
  }

  @Trace
  def getSecondNumber: Int = {
    2
  }

  @Trace
  def getThirdNumber: Int = {
    3
  }
}
