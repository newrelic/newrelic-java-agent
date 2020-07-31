/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.SkipIfPresent;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveWithAnnotation;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts weave annotation information from a {@link ClassNode}.
 */
public class WeaveClassInfo {
    /**
     * Descriptor for a {@link SkipIfPresent} annotation.
     */
    public static final String SKIP_IF_PRESENT_DESC = Type.getType(SkipIfPresent.class).getDescriptor();

    /**
     * Descriptor for a {@link Weave} annotation.
     */
    public static final String WEAVE_DESC = Type.getType(Weave.class).getDescriptor();

    /**
     * Descriptor for a {@link WeaveWithAnnotation} annotation.
     */
    public static final String WEAVE_ANNOTATED_TYPE_DESC = Type.getType(WeaveWithAnnotation.class).getDescriptor();

    private final List<WeaveViolation> violations = new ArrayList<>();
    private MatchType matchType;
    private String originalName;
    private boolean skipIfPresent;
    private final Set<String> requiredClassAnnotations = new HashSet<>();
    private final Set<String> requiredMethodAnnotations = new HashSet<>();

    /**
     * Extract the weave annotation information from the specified {@link ClassNode}.
     *
     * @param weaveClassNode class node to extract information from
     */
    public WeaveClassInfo(ClassNode weaveClassNode) {
        if (weaveClassNode.version > WeaveUtils.RUNTIME_MAX_SUPPORTED_CLASS_VERSION) {
            violations.add(new WeaveViolation(WeaveViolationType.INCOMPATIBLE_BYTECODE_VERSION, weaveClassNode.name));
        }

        this.matchType = null;
        if (weaveClassNode.visibleAnnotations != null) {
            for (AnnotationNode annotationNode : weaveClassNode.visibleAnnotations) {
                if (annotationNode.desc.equals(WEAVE_DESC)) {
                    matchType = MatchType.ExactClass;
                    originalName = weaveClassNode.name;
                    processAnnotationValues(annotationNode, null);
                } else if (annotationNode.desc.equals(SKIP_IF_PRESENT_DESC)) {
                    skipIfPresent = true;
                    originalName = weaveClassNode.name;
                    processAnnotationValues(annotationNode, null);
                } else if (annotationNode.desc.equals(WEAVE_ANNOTATED_TYPE_DESC)) {
                    matchType = MatchType.ExactClass;
                    processAnnotationValues(annotationNode, requiredClassAnnotations);
                }
            }
        }

        // Check for method-level @WeaveWithAnnotation
        for (MethodNode methodNode : weaveClassNode.methods) {
            if (methodNode.visibleAnnotations != null) {
                for (AnnotationNode annotationNode : methodNode.visibleAnnotations) {
                    if (annotationNode.desc.equals(WEAVE_ANNOTATED_TYPE_DESC)) {
                        processAnnotationValues(annotationNode, requiredMethodAnnotations);
                    }
                }
            }
        }
    }

    private void processAnnotationValues(AnnotationNode annotationNode, Set<String> annotationWeavingType) {
        if (annotationNode.values != null) {
            // look through key-value pairs of the weave annotation
            for (int i = 0; i < annotationNode.values.size(); i += 2) {
                if ("type".equals(annotationNode.values.get(i))) {
                    String[] typeParameters = (String[]) annotationNode.values.get(i + 1);
                    if (Type.getType(MatchType.class).getDescriptor().equals(typeParameters[0])) {
                        matchType = MatchType.valueOf(typeParameters[1]);
                    }
                } else if ("originalName".equals(annotationNode.values.get(i))) {
                    originalName = WeaveUtils.getClassInternalName(String.valueOf(annotationNode.values.get(i + 1)));
                } else if (annotationWeavingType != null && WeaveUtils.ANNOTATION_CLASSES_ATTRIBUTE_KEY.equals(annotationNode.values.get(i))) {
                    List<String> requiredAnnotationClasses = (List<String>) annotationNode.values.get(i + 1);
                    annotationWeavingType.addAll(requiredAnnotationClasses);
                }
            }
        }
    }

    /**
     * The match type from the {@link Weave} annotation, or <code>null</code> if the annotation was not present.
     *
     * @return match type from the {@link Weave} annotation, or <code>null</code> if the annotation was not present
     */
    public MatchType getMatchType() {
        return matchType;
    }

    /**
     * The original name for the weave class, or <code>null</code> if the {@link Weave} annotation was not present.
     *
     * @return original name for the weave class, or <code>null</code> if the {@link Weave} annotation was not present
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * Any violations that occurred when processing the {@link ClassNode}.
     *
     * @return violations that occurred when processing the {@link ClassNode}
     */
    public List<WeaveViolation> getViolations() {
        return violations;
    }

    /**
     * Whether or not the class was annotated with a {@link SkipIfPresent} annotation.
     *
     * @return <code>true</code> if the class was annotated with a {@link SkipIfPresent} annotation
     */
    public boolean isSkipIfPresent() {
        return skipIfPresent;
    }

    /**
     * The set of annotation class names that are required on a class in order to match. This information is extracted
     * from a {@link WeaveWithAnnotation} annotation on this class.
     *
     * @return a set of class names that are required to match this instrumentation
     */
    public Set<String> getRequiredClassAnnotations() {
        return requiredClassAnnotations;
    }

    public Set<String> getRequiredMethodAnnotations() {
        return requiredMethodAnnotations;
    }
}
