package com.nr.agent.instrumentation.scala.baseline

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector}
import com.newrelic.api.agent.Trace
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class TransactionSynchronousTest {

  @Test
  def oneTransaction(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val firstNumber = getFirstNumber
    val result = firstNumber

    //Then
    Assert.assertEquals("Result", 1, result)
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
  }

  @Test
  def twoTransactions(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val firstNumber = getFirstNumber
    val secondNumber = getSecondNumber
    val result = firstNumber + secondNumber

    //Then
    Assert.assertEquals("Result", 3, result)
    Assert.assertEquals("Transactions", 2, introspector.getTransactionNames.size)
  }

  @Test
  def threeTransactions(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val firstNumber = getFirstNumber
    val secondNumber = getSecondNumber
    val thirdNumber = getThirdNumber
    val result = firstNumber + secondNumber + thirdNumber

    //Then
    Assert.assertEquals("Result", 6, result)
    Assert.assertEquals("Transactions", 3, introspector.getTransactionNames.size)
  }

  @Trace(dispatcher = true)
  def getFirstNumber: Int = {
    1
  }

  @Trace(dispatcher = true)
  def getSecondNumber: Int = {
    2
  }

  @Trace(dispatcher = true)
  def getThirdNumber: Int = {
    3
  }
}
