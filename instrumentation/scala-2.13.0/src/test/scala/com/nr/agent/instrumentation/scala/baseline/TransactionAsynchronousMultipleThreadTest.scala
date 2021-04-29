package com.nr.agent.instrumentation.scala.baseline

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector}
import com.newrelic.api.agent.Trace
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class TransactionAsynchronousMultipleThreadTest {

  val threadPoolOne: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
  val threadPoolTwo: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
  val threadPoolThree: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  @Test
  def oneTransaction(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    implicit val ec: ExecutionContext = threadPoolOne

    //When
    val result = getFirstNumber.map(firstNumber => firstNumber)

    //Then
    Assert.assertEquals("Result", 1, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
  }

  @Test
  def twoTransactions(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    implicit val ec: ExecutionContext = threadPoolOne

    //When
    val result = getFirstNumber.flatMap(firstNumber =>
      getSecondNumber.map(secondNumber =>
        firstNumber + secondNumber
      )
    )

    //Then
    Assert.assertEquals("Result", 3, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 2, introspector.getTransactionNames.size)
  }

  @Test
  def threeTransactions(): Unit = {
    //Given
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    implicit val ec: ExecutionContext = threadPoolOne

    //When
    val result = getFirstNumber.flatMap(firstNumber =>
      getSecondNumber.flatMap(secondNumber =>
        getThirdNumber.map(thirdNumber =>
          firstNumber + secondNumber + thirdNumber
        )
      )
    )

    //Then
    Assert.assertEquals("Result", 6, Await.result(result, 2.seconds))
    Assert.assertEquals("Transactions", 3, introspector.getTransactionNames.size)
  }

  @Trace(dispatcher = true)
  def getFirstNumber: Future[Int] = Future {
    1
  }(threadPoolOne)

  @Trace(dispatcher = true)
  def getSecondNumber: Future[Int] = Future {
    2
  }(threadPoolTwo)

  @Trace(dispatcher = true)
  def getThirdNumber: Future[Int] = Future {
    3
  }(threadPoolThree)
}
