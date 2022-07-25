/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.language.scala;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.scala.ScalaMatchType;
import com.newrelic.api.agent.weaver.scala.ScalaWeave;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.utils.WeaveClassInfo;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.weavepackage.language.LanguageAdapter;
import com.newrelic.weave.weavepackage.language.LanguageAdapterResult;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScalaAdapter implements LanguageAdapter {
    private static final String WEAVE_API_DESC = Type.getType(Weave.class).getDescriptor();
    private static final String SCALA_WEAVE_API_DESC =  Type.getType(ScalaWeave.class).getDescriptor();
    private static final String MATCH_TYPE_DESC = Type.getType(MatchType.class).getDescriptor();
    private static final String SCALA_MATCH_TYPE_DESC = Type.getType(ScalaMatchType.class).getDescriptor();

    @Override
    public LanguageAdapterResult adapt(List<byte[]> input) {
        List<WeaveViolation> violations = new ArrayList<>();
        // internalName -> classNode
        Map<String, ClassNode> scalaNodes = new HashMap<>(input.size());
        List<ClassNode> outputNodes = new ArrayList<>(input.size());
        for(byte[] bytes : input) {
            ClassNode node = WeaveUtils.convertToClassNode(bytes);
            if(isScalaNode(node)) {
                scalaNodes.put(node.name, node);
            } else{
                // don't alter non-scala sources
                outputNodes.add(node);
            }
        }
        if (scalaNodes.size() == 0) {
            return new ScalaAdapterResult(input, violations);
        }

        // scala-weaver-api -> weaver-api
        apiConversions(scalaNodes);
        // check for violations on scala nodes
        violations.addAll(validateScalaNodes(scalaNodes));

        outputNodes.addAll(scalaNodes.values());
        return new ScalaAdapterResult(convertToBytes(outputNodes), violations);
    }

    private List<byte[]> convertToBytes(List<ClassNode> nodes) {
        ClassCache packageCache = new ClassCache(new ClassLoaderFinder(Thread.currentThread().getContextClassLoader()));
        List<byte[]> converted = new ArrayList<>(nodes.size());
        for(ClassNode node : nodes) {
            converted.add(WeaveUtils.convertToClassBytes(node, packageCache));
        }
        return converted;
    }

    private boolean isScalaNode(ClassNode node) {
        if(node.sourceFile != null && node.sourceFile.endsWith(".scala")) {
            return true;
        }
        return false;
    }

    private void apiConversions(Map<String, ClassNode> input) {
        for(ClassNode node : input.values()) {
            if(null != node.visibleAnnotations) {
                for(AnnotationNode annotation : node.visibleAnnotations) {
                    if (SCALA_WEAVE_API_DESC.equals(annotation.desc)) {
                        annotation.desc = WEAVE_API_DESC;
                        int originalNameIndex = -1;
                        int matchTypeIndex = -1;
                        for (int i = 0; i < annotation.values.size(); ++i) {
                            Object key = annotation.values.get(i);
                            if (key instanceof String) {
                                if ("type".equals(key)) {
                                    matchTypeIndex = i;
                                } else if ("originalName".equals(key)) {
                                    originalNameIndex = i;
                                }
                            }
                        }
                        if (matchTypeIndex != -1) {
                            String[] matchVal = (String[])annotation.values.get(matchTypeIndex+1);
                            String scalaMatchType = matchVal[1];
                            matchVal[0] = MATCH_TYPE_DESC;
                            matchVal[1] = convertToJavaMatchType(matchVal[1]);
                            if ("Object".equals(scalaMatchType)) {
                                if (originalNameIndex != -1) {
                                    annotation.values.set(originalNameIndex+1, annotation.values.get(originalNameIndex+1)+"$");
                                } else {
                                    annotation.values.add("originalName");
                                    annotation.values.add(WeaveUtils.getClassBinaryName(node.name)+"$");
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Scan Scala ClassNodes for any violations.
     *
     * @param input The nodes to check
     * @return A list of all violations encountered
     */
    private List<WeaveViolation> validateScalaNodes(Map<String, ClassNode> input) {
      // check that the user only annotated scala classes with @Weave or @ScalaWeave
      List<WeaveViolation> violations = new ArrayList<>();
      for (String name : input.keySet()) {
        ClassNode node = input.get(name);
        boolean isWeaveClass = isWeaveClass(node);
        if (isWeaveClass && isInterface(node)) {
          violations.add(new ScalaWeaveViolation(name, ScalaWeaveViolationType.CLASS_WEAVE_IS_TRAIT));
        }
        if (isWeaveClass && name.endsWith("$")) {
          // check for object
          String implName = name.substring(0, name.length() - 1);
          if (input.containsKey(implName)) {
            violations.add(new ScalaWeaveViolation(name, ScalaWeaveViolationType.CLASS_WEAVE_IS_OBJECT));
          }
        }
      }
      return violations;
    }

    private boolean isInterface(ClassNode node) {
      return (node.access & Opcodes.ACC_INTERFACE) != 0;
    }

    private boolean isWeaveClass(ClassNode node) {
        if (null != node && null != node.visibleAnnotations) {
            for (AnnotationNode annotation : node.visibleAnnotations) {
                if (SCALA_WEAVE_API_DESC.equals(annotation.desc) || WeaveClassInfo.WEAVE_DESC.equals(annotation.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String convertToJavaMatchType(String scalaMatch) {
        if (ScalaMatchType.Trait.name().equals(scalaMatch)) {
            return MatchType.Interface.name();
        } else {
            return MatchType.ExactClass.name();
        }
    }

    private static class ScalaAdapterResult implements LanguageAdapterResult {
        private final List<byte[]> adaptedBytes;
        private final List<WeaveViolation> violations;

        public ScalaAdapterResult(List<byte[]> adaptedBytes, List<WeaveViolation> violations) {
            this.adaptedBytes = adaptedBytes;
            this.violations = violations;
        }

        @Override
        public List<byte[]> getAdaptedBytes() {
            return adaptedBytes;
        }

        @Override
        public List<WeaveViolation> getViolations() {
            return violations;
        }
    }

}
