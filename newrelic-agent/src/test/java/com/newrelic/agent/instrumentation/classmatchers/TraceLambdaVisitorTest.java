package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.api.agent.TraceLambda;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Method;

import java.io.IOException;
import java.util.Map;

public class TraceLambdaVisitorTest {
    @Test
    public void traceJavaLambdaTest() throws IOException {
        validateTraceAnnotations(visit(JavaLambdaExample.class), "lambda$example$0", "example.0");
    }

    @Test
    public void traceScalaLambdaTest() throws IOException {
        validateTraceAnnotations(visit(ScalaLambdaExample.class), "$anonfun$example$0", "example.0");
    }

    @Test
    public void tracePatternTest() throws IOException {
        validateTraceAnnotations(visit(PatternExample.class), "customPattern", "customPattern");
    }

    @Test
    public void traceNonstaticTest() throws IOException {
        validateTraceAnnotations(visit(NonstaticExample.class), "lambda$example$0", "example.0");
    }

    @Test
    public void tracePatternNonstaticTest() throws IOException {
        validateTraceAnnotations(visit(PatternNonstaticExample.class), "customPattern", "customPattern");
    }

    private InstrumentationContext visit(Class<?> clazz) throws IOException {
        InstrumentationContext context = new InstrumentationContext(null, clazz, clazz.getProtectionDomain());
        ClassReader reader = new ClassReader(clazz.getName());
        ClassVisitor visitor = new TraceLambdaVisitor().newClassMatchVisitor(clazz.getClassLoader(), clazz, reader, new ClassWriter(0), context);
        reader.accept(visitor, 0);
        return context;
    }

    private void validateTraceAnnotations(InstrumentationContext context, String methodName, String metricName) {
        Map<Method, TraceDetails> traceAnnotations = context.getTraceInformation().getTraceAnnotations();
        Assert.assertFalse(traceAnnotations.isEmpty());
        Map.Entry<Method, TraceDetails> traceAnnotation = traceAnnotations.entrySet().iterator().next();
        Assert.assertEquals(methodName, traceAnnotation.getKey().getName());
        Assert.assertEquals(metricName, traceAnnotation.getValue().metricName());
    }
}

@TraceLambda
class JavaLambdaExample {
    public static void lambda$example$0() {}
}

@TraceLambda
class ScalaLambdaExample {
    public static void $anonfun$example$0() {}
}

@TraceLambda(pattern = "^customPattern")
class PatternExample {
    public static void customPattern() {}
}

@TraceLambda(includeNonstatic = true)
class NonstaticExample {
    public void lambda$example$0() {}
}

@TraceLambda(pattern = "^customPattern", includeNonstatic = true)
class PatternNonstaticExample {
    public void customPattern() {}
}

