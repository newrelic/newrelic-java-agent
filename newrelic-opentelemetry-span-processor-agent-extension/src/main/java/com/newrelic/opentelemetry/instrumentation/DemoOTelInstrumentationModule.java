/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentelemetry.instrumentation;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * This is a demo instrumentation which hooks into servlet invocation and modifies the http
 * response.
 */
@AutoService(InstrumentationModule.class)
public final class DemoOTelInstrumentationModule extends InstrumentationModule {
  public DemoOTelInstrumentationModule() {
    super("otel-demo", "otel");
  }

  /*
  We want this instrumentation to be applied after the standard servlet instrumentation.
  The latter creates a server span around http request.
  This instrumentation needs access to that server span.
   */
  @Override
  public int order() {
    return 1;
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return AgentElementMatchers.hasClassesNamed("org.springframework.samples.petclinic.repro.ReproTwoTransactions");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new DemoOTelInstrumentation());
  }
}
