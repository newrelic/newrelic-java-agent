/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.weave.weavepackage.language.scala

import org.junit.{Assert, Test}
import com.newrelic.agent.instrumentation.context.{InstrumentationContext, LambdaTraceVisitor}
import com.newrelic.api.agent.LambdaTrace
import com.newrelic.agent.deps.org.objectweb.asm.{ClassReader, ClassWriter}

class LambdaTraceVisitorTest {
  @Test
  def testAnnotatedClass(): Unit = {
    val context = new InstrumentationContext(null, classOf[AnnotatedClass], classOf[AnnotatedClass].getProtectionDomain)
    val reader = new ClassReader(classOf[AnnotatedClass].getName)
    val target = new LambdaTraceVisitor()
    val visitor = target.newClassMatchVisitor(classOf[AnnotatedClass].getClassLoader, classOf[AnnotatedClass], reader, new ClassWriter(0), context)
    reader.accept(visitor, 0)

    val traceInformation = context.getTraceInformation
    val traceAnnotations = traceInformation.getTraceAnnotations

    System.out.println(context)
    System.out.println(traceInformation)
    System.out.println(traceAnnotations)
  }

  @LambdaTrace()
  class AnnotatedClass {
    val addOne: Int => Int = x => x + 1
  }
}
