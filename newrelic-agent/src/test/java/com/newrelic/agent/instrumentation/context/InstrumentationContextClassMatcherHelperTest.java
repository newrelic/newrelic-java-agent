/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InstrumentationContextClassMatcherHelperTest {

  InstrumentationContextClassMatcherHelper testClass = new InstrumentationContextClassMatcherHelper();

  @Test
  public void MissingResourceExpectedStartsWithComNewRelic() {
    assertTrue(
        testClass.isMissingResourceExpected("com.newrelic.agent.instrumentation.WeavedMethod"));
  }

  @Test
  public void MissingResourceExpectedStartsWithWeave() {
    assertTrue(testClass.isMissingResourceExpected("weave.asm.WeaveMethod"));
  }

  @Test
  public void MissingResourceExpectedStartsWithComNrInstrumentation() {
    assertTrue(testClass.isMissingResourceExpected("com.nr.instrumentation.servlet24.NRRequestWrapper"));
  }

  @Test
  public void MissingResourceExpectedStartsWithLambdas() {
    assertTrue(testClass.isMissingResourceExpected(
        "sun.util.locale.provider.JRELocaleProviderAdapter$$Lambda$108"));
    assertTrue(testClass.isMissingResourceExpected("java.lang.invoke.LambdaForm$MH"));
    assertTrue(testClass.isMissingResourceExpected("java.util.stream.Collectors$$Lambda/0x800000047"));
  }

  @Test
  public void MissingResourceExpectedStartsWithConstructorAccess() {
    assertTrue(
        testClass.isMissingResourceExpected("jdk.internal.reflect.GeneratedConstructorAccessor6"));
  }

  @Test
  public void MissingResourceExpectedStartsWithMethodAccess() {
    assertTrue(
        testClass.isMissingResourceExpected("jdk.internal.reflect.GeneratedMethodAccessor2"));
  }

  @Test
  public void MissingResourceExpectedStartsWithBoundMethodHandle() {
    assertTrue(testClass
        .isMissingResourceExpected("java.lang.invoke.BoundMethodHandle$Species_LLLLLLLLIILL"));
  }

  @Test
  public void MissingResourceExpectedStartsWithJdkJfr() {
    assertTrue(testClass.isMissingResourceExpected("jdk.jfr.internal.handlers.EventHandler"));
  }

}
