package com.newrelic.agent.instrumentation.custom;

import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.util.asm.CustomClassLoaderClassWriter;
import com.newrelic.agent.util.asm.PatchedClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Level;

import static com.newrelic.agent.Agent.LOG;

public class ScalaTraitFinalFieldTransformer implements ContextClassTransformer {
  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context, OptimizedClassMatcher.Match match) throws IllegalClassFormatException {
    try {
      return doTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer, context);
    } catch (Throwable t) {
      LOG.log(Level.FINE, "Unable to transform class " + className, t);
      return null;
    }
  }


  private byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                             ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context) {

    if (context.getScalaFinalFields().isEmpty()) {
      return null;
    }

    LOG.debug("Instrumenting class " + className);
    ClassReader reader = new ClassReader(classfileBuffer);
    ClassWriter writer = new CustomClassLoaderClassWriter(ClassWriter.COMPUTE_FRAMES, loader);
    ClassVisitor cv = writer;
    cv = new ScalaTraitFinalFieldVisitor(cv, context.getScalaFinalFields());
    reader.accept(cv, ClassReader.SKIP_FRAMES);

    return writer.toByteArray();
  }
}
