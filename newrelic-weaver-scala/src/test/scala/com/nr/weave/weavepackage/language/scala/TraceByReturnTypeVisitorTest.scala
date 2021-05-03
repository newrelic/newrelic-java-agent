/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.weave.weavepackage.language.scala

import java.util.concurrent.Executors
import java.util.{Set => JSet}

import com.newrelic.api.agent.{TraceByReturnType, Trace}
import org.junit.{Assert, Test}
import com.newrelic.agent.instrumentation.context.{InstrumentationContext, TraceByReturnTypeMatchVisitor}
import org.hamcrest.Matchers.{contains, containsInAnyOrder, not}
import com.newrelic.agent.deps.org.objectweb.asm.{ClassReader, ClassWriter, Type}
import com.newrelic.agent.deps.org.objectweb.asm.commons.Method
import collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}


class TraceByReturnTypeVisitorTest {
  @Test
  def testFutureReturnType(): Unit = {

    val context = instrumentClass(classOf[FutureAnnotatedClass])
    val traceInformation = context.getTraceInformation
    val tracedMethods: JSet[Method] = traceInformation.getTraceAnnotations.keySet()
    val tracedMethodNames: JSet[String] = methodNames(tracedMethods)

    Assert.assertTrue("all traced methods should return Future",
      allMatchTypeDescriptor(tracedMethods, classOf[Future[_]])
    )
    Assert.assertThat(tracedMethodNames,
      containsInAnyOrder("twoFuture",
        "oneFuture")
    )
    Assert.assertThat(tracedMethodNames, not(contains("returnString", "returnsInt", "oneOption")))
  }

  @Test
  def testFutureAndOptionReturnType(): Unit = {

    val context = instrumentClass(classOf[OptionAndFutureAnnotatedClass])
    val traceInformation = context.getTraceInformation
    val tracedMethods: JSet[Method] = traceInformation.getTraceAnnotations.keySet()
    val tracedMethodNames: JSet[String] = methodNames(tracedMethods)

    Assert.assertTrue("all traced methods should return Future or Option",
      allMatchTypeDescriptor(tracedMethods, classOf[Future[_]], classOf[Option[_]])
    )
    Assert.assertThat(tracedMethodNames,
      containsInAnyOrder("twoFuture",
        "oneFuture",
        "oneOption")
    )
    Assert.assertThat(tracedMethodNames, not(contains("returnString", "returnsInt")))
  }

  private def methodNames(traceMethods: JSet[Method]): JSet[String] =
    traceMethods.asScala.map(_.getName).asJava

  private def allMatchTypeDescriptor(methods: JSet[Method], clazzes: Class[_]*): Boolean = {
    val methodDescriptorMatchesOneClass: Method => Boolean =
      method => clazzes.toList.exists(clazz => method.getDescriptor.endsWith(Type.getDescriptor(clazz)))
    methods.asScala.forall(methodDescriptorMatchesOneClass)
  }

  private def instrumentClass(clazz: Class[_]) = {
    val context = new InstrumentationContext(null, clazz, clazz.getProtectionDomain)
    val reader = new ClassReader(clazz.getName)
    val target = new TraceByReturnTypeMatchVisitor()
    val visitor = target.newClassMatchVisitor(clazz.getClassLoader, clazz, reader, new ClassWriter(0), context)
    reader.accept(visitor, 0)
    context
  }

  @TraceByReturnType(traceReturnTypes = Array(classOf[Future[_]], classOf[Option[_]]))
  class OptionAndFutureAnnotatedClass {
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

    def returnInt: Int = 1

    def returnString: String = "String"

    def oneFuture(i: Int): Future[Int] = Future.successful(i)

    def twoFuture(i: Int): Future[Int] = Future.successful(i).map(_ + 2)

    def oneOption(i: Int): Option[Int] = Option(i)
  }

  @TraceByReturnType(traceReturnTypes = Array(classOf[Future[_]]))
  class FutureAnnotatedClass {
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

    def returnInt: Int = 1

    def returnString: String = "String"

    def oneFuture(i: Int): Future[Int] = Future.successful(i)

    def twoFuture(i: Int): Future[Int] = oneFuture(i).map(_ + 2)

    def oneOption(i: Int): Option[Int] = Option(i)
  }

}

