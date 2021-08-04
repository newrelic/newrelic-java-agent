/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.TraceByReturnTypeMatchVisitor;
import com.newrelic.api.agent.TraceByReturnType;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;

public class TraceByReturnTypeMatchVisitorTest {

  @Test
  public void testFutureReturnType() throws IOException {
    InstrumentationContext context = instrumentClass(JavaFutureInstrumented.class);
    Set<Method> tracedMethods = context.getTraceInformation().getTraceAnnotations().keySet();
    Set<String> tracedMethodNames = methodNames(tracedMethods);
    Assert.assertTrue(allMatchTypeDescriptor(tracedMethods, Future.class));
    Assert.assertThat(tracedMethodNames, containsInAnyOrder("returnFuture", "submitFuture", "access$000"));
    Assert.assertThat(tracedMethodNames, not(contains("returnString", "returnInteger")));
  }

  @Test
  public void testStringReturnType() throws IOException {
    InstrumentationContext context = instrumentClass(StringInstrumented.class);
    Set<Method> tracedMethods = context.getTraceInformation().getTraceAnnotations().keySet();
    Set<String> tracedMethodNames = methodNames(tracedMethods);
    Assert.assertTrue(allMatchTypeDescriptor(tracedMethods, String.class));
    Assert.assertThat(tracedMethodNames, containsInAnyOrder("returnString"));
    Assert.assertThat(tracedMethodNames, not(contains("returnInteger")));
  }

  @Test
  public void testFutureAndStringReturnType() throws IOException {
    InstrumentationContext context = instrumentClass(FutureAndStringInstrumented.class);
    Set<Method> tracedMethods = context.getTraceInformation().getTraceAnnotations().keySet();
    Set<String> tracedMethodNames = methodNames(tracedMethods);
    Assert.assertTrue(allMatchTypeDescriptor(tracedMethods, Future.class, String.class));
    Assert.assertThat(tracedMethodNames, containsInAnyOrder("returnString", "returnFuture"));
    Assert.assertThat(tracedMethodNames, not(contains("returnInteger")));
  }

  private boolean allMatchTypeDescriptor(Set<Method> methods, final Class<?>... clazzes) {
    Predicate<Method> methodDescriptorMatchesOneClass = new Predicate<Method>() {
      @Override
      public boolean test(final Method method) {
        return Arrays.stream(clazzes).anyMatch(new Predicate<Class<?>>() {
          @Override
          public boolean test(Class<?> clazz) {
            return method.getDescriptor().endsWith(Type.getDescriptor(clazz));
          }
        });
      }
    };
    return methods.stream().allMatch(methodDescriptorMatchesOneClass);
  }

  private Set<String> methodNames(Set<Method> methods) {
    return methods.stream().map(new Function<Method, String>() {
      @Override
      public String apply(Method method) {
        return method.getName();
      }
    }).collect(Collectors.<String>toSet());
  }

  private InstrumentationContext instrumentClass(Class<?> clazz) throws IOException {
    InstrumentationContext context = new InstrumentationContext(null, clazz, clazz.getProtectionDomain());
    ClassReader reader = new ClassReader(clazz.getName());
    ClassVisitor visitor = new TraceByReturnTypeMatchVisitor().newClassMatchVisitor(clazz.getClassLoader(), clazz, reader, new ClassWriter(0), context);
    reader.accept(visitor, 0);
    return context;
  }

  @TraceByReturnType(traceReturnTypes = Future.class)
  class JavaFutureInstrumented {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    //becomes static method access$000
    public Function<Integer, Future<Integer>> futureFunction =
      new Function<Integer, Future<Integer>>() {
        @Override
        public Future<Integer> apply(Integer i) {
          return JavaFutureInstrumented.this.submitFuture(i);
        }
      };

    public Future<Integer> returnFuture() {
      return submitFuture(1);
    }

    private <T> Future<T> submitFuture(final T t) {
      return executorService.submit(new Callable<T>() {
        @Override
        public T call() throws Exception {
          return t;
        }
      });
    }

    public String returnString() {
      return "";
    }

    public Integer returnInteger() {
      return 0;
    }
  }

  @TraceByReturnType(traceReturnTypes = String.class)
  class StringInstrumented {

    public String returnString() {
      return "";
    }

    public Integer returnInteger() {
      return 0;
    }
  }

  @TraceByReturnType(traceReturnTypes = {Future.class, String.class})
  class FutureAndStringInstrumented {

    public String returnString() {
      return "";
    }

    public Future<Integer> returnFuture() { return null;}

    public Integer returnInteger() {
      return 0;
    }
  }
}
