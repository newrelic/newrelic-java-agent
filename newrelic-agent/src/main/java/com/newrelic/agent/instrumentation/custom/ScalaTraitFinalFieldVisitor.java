package com.newrelic.agent.instrumentation.custom;

import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Set;

public class ScalaTraitFinalFieldVisitor extends ClassVisitor {
  private final Set<String> finalFieldNames;

  public ScalaTraitFinalFieldVisitor(ClassVisitor cv,  Set<String> finalFieldNames) {
    super(WeaveUtils.ASM_API_LEVEL, cv);
    this.finalFieldNames = finalFieldNames;
  }

  @Override
  public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
    if(finalFieldNames.contains(name)) {
      access &= ~Opcodes.ACC_FINAL;
    }
    return super.visitField(access, name, descriptor, signature, value);
  }
}
