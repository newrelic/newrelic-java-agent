/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentelemetry.instrumentation;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;

public class DemoOTelInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return AgentElementMatchers.implementsInterface(
                namedOneOf("org.springframework.samples.petclinic.repro.ReproTwoTransactions"));
    }

    @Override
    public void transform(TypeTransformer typeTransformer) {
        // private Context doStart(Context parentContext, REQUEST request, @Nullable Instant startTime) {
        typeTransformer.applyAdviceToMethod(
                namedOneOf("success", "hello"),
                this.getClass().getName() + "$DemoServlet3Advice");
    }

    @SuppressWarnings("unused")
    public static class DemoServlet3Advice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter() {
            System.out.println("JASON was here");

//      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
//      if (!httpServletResponse.containsHeader("X-server-id")) {
//        httpServletResponse.setHeader(
//            "X-server-id", Java8BytecodeBridge.currentSpan().getSpanContext().getTraceId());
//      }
        }
    }
}
