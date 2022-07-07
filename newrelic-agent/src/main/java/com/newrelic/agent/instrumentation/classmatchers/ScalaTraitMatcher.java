package com.newrelic.agent.instrumentation.classmatchers;


import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scala traits (interfaces) produce a static synthetic method `Interface#method$`
 * These are called by implementing classes if not overridden. We skip mark the child class
 * method call to be skipped from weaving if the trait is weaved to prevent a "Double weave"
 *
 * trait example with synthetic method
 *
 * Scala trait `SomeTrait` converted to Java
 *
 * ```Scala
 *  trait SomeTrait {
 *     def traitmethod(): String = {
 *       "original"
 *     }
 *   }
 * ```
 * ```Java
 * public interface SomeTrait {
 *    // $FF: synthetic method
 *    static String traitmethod$(final SomeTrait $this) {
 *       return $this.traitmethod();
 *    }
 *    default String traitmethod() {
 *       return "original";
 *    }
 *    static void $init$(final SomeTrait $this) {
 *    }
 * }
 * ```
 */
public class ScalaTraitMatcher implements ClassMatchVisitorFactory {

  @Override
  public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader, ClassVisitor cv, InstrumentationContext context) {
    return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {
      private boolean isScalaSource;
      private List<String> interfaces;
      private String className;

      @Override
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.interfaces = interfaces != null ? Arrays.asList(interfaces) : new ArrayList<>();
        this.className = name;
      }

      @Override
      public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        this.isScalaSource = source.endsWith(".scala");
      }

      @Override
      public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if(isScalaSource && this.interfaces.size() > 0 && ((access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL)) {
          context.addScalaFinalField(name);
        }
        return super.visitField(access, name, descriptor, signature, value);
      }

      @Override
      public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);
        if (isScalaSource && this.interfaces.size() > 0) {
          mv = new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
              if (itf && opcode == Opcodes.INVOKESTATIC &&
                  (methodName + "$").equals(name) &&
                  !owner.equalsIgnoreCase(className)) {
                context.putMatch(ScalaTraitMatcher.this, null);
                context.addSkipMethod(new Method(methodName, methodDesc), owner);
              }
              super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
          };
        }
        return mv;
      }
    };
  }
}
