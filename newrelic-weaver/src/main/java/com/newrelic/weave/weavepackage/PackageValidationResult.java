/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.google.common.collect.Queues;
import com.newrelic.weave.ClassMatch;
import com.newrelic.weave.ClassWeave;
import com.newrelic.weave.MethodProcessors;
import com.newrelic.weave.PreparedExtension;
import com.newrelic.weave.PreparedMatch;
import com.newrelic.weave.WeaveViolationFilter;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassInformation;
import com.newrelic.weave.utils.SynchronizedClassNode;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.violation.ReferenceViolation;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * The result of matching a {@link WeavePackage} against a {@link ClassCache}.
 */
public class PackageValidationResult {
    private static final Pattern WEAVE_PACKAGE_PATTERN = Pattern.compile("^com.newrelic.weave..*");
    public static final Comparator<PackageValidationResult> CONFIG_COMPARATOR = Comparator.comparing(PackageValidationResult::weavePackageConfig);
    private final Queue<WeaveViolation> violations = Queues.newConcurrentLinkedQueue();
    private final Map<String, ClassNode> utilClasses = new ConcurrentHashMap<>();
    private final Map<String, ClassNode> allAnnotationClasses = new ConcurrentHashMap<>();
    private final Map<String, ClassNode> baseAnnotationClasses = new ConcurrentHashMap<>();
    private final Map<String, ClassNode> allMethodAnnotationClasses = new ConcurrentHashMap<>();
    private final Map<String, PreparedMatch> exactMatches = new ConcurrentHashMap<>();
    private final Map<String, PreparedMatch> baseMatches = new ConcurrentHashMap<>();
    private final WeavePackage weavePackage;
    private final WeaveViolationFilter weaveViolationFilter;

    private ClassNode errorHandler;

    /**
     * Construct a full ValidationResult
     */
    public PackageValidationResult(WeavePackage weavePackage, ClassCache cache, Collection<Reference> references,
            Map<String, ClassNode> exactWeaves, Map<String, ClassNode> baseWeaves,
            Map<String, ClassNode> allAnnotationClasses, Map<String, ClassNode> baseAnnotationClasses,
            Map<String, ClassNode> allMethodAnnotationClasses, Map<String, ClassNode> utilClasses,
            Set<String> skipIfPresentNames, ClassNode errorHandler,
            ClassNode extensionTemplate) throws IOException {

        this.weavePackage = weavePackage;
        this.errorHandler = errorHandler;
        this.utilClasses.putAll(utilClasses);
        this.allAnnotationClasses.putAll(allAnnotationClasses);
        this.baseAnnotationClasses.putAll(baseAnnotationClasses);
        this.allMethodAnnotationClasses.putAll(allMethodAnnotationClasses);
        this.weaveViolationFilter = this.weavePackage.getConfig().getWeaveViolationFilter();

        this.skipIfPresent(skipIfPresentNames, cache);

        // make sure our weave code and util classes reference original code correctly
        this.validateReferences(cache, references);

        // load up resources from classloader and verify against weave bytes
        this.processMatches(cache, exactWeaves, exactMatches, false, errorHandler);
        this.processMatches(cache, baseWeaves, baseMatches, true, errorHandler);

        for (String utilClassName : this.utilClasses.keySet()) {
            if (cache.hasClassResource(utilClassName) && (!WEAVE_PACKAGE_PATTERN.matcher(utilClassName).matches())) {
                violations.add(new WeaveViolation(WeaveViolationType.ILLEGAL_CLASS_NAME, utilClassName));
            }
        }
        for (Entry<String, ClassNode> entry : this.utilClasses.entrySet()) {
            if (null == entry.getValue()) {
                violations.add(new WeaveViolation(WeaveViolationType.MISSING_ORIGINAL_BYTECODE, entry.getKey()));
            }
        }
        rewriteAllNewFieldCalls();
        if (!succeeded()) {
            this.exactMatches.clear();
            this.baseMatches.clear();
            this.utilClasses.clear();
            this.allAnnotationClasses.clear();
            this.baseAnnotationClasses.clear();
            this.allMethodAnnotationClasses.clear();
        }
    }

    /**
     * Construct a fail-fast ValidationResult
     */
    public PackageValidationResult(WeavePackage weavePackage, ClassCache cache, Set<String> requiredClasses,
            Set<String> illegalClasses) {
        this.weavePackage = weavePackage;
        this.weaveViolationFilter = this.weavePackage.getConfig().getWeaveViolationFilter();
        for (String requiredClass : requiredClasses) {
            if (!cache.hasClassResource(requiredClass)) {
                violations.add(new WeaveViolation(WeaveViolationType.MISSING_ORIGINAL_BYTECODE, requiredClass));
                return;
            }
        }
        for (String illegalClass : illegalClasses) {
            if (cache.hasClassResource(illegalClass)) {
                violations.add(new WeaveViolation(WeaveViolationType.SKIP_IF_PRESENT, illegalClass));
                return;
            }
        }
    }

    /**
     * Construct a failed PackageValidationResult.
     *
     * @param weavePackage The WeavePackage to fail.
     * @param packageViolations The violations to fail with.
     */
    PackageValidationResult(WeavePackage weavePackage, Queue<WeaveViolation> packageViolations) {
        this.weavePackage = weavePackage;
        this.weaveViolationFilter = this.weavePackage.getConfig().getWeaveViolationFilter();
        this.violations.addAll(packageViolations);
    }

    private void skipIfPresent(Set<String> skipIfPresentNames, ClassCache cache) {
        for (String skipIfPresentName : skipIfPresentNames) {
            if (cache.hasClassResource(skipIfPresentName)) {
                violations.add(new WeaveViolation(WeaveViolationType.SKIP_IF_PRESENT, skipIfPresentName));
            }
        }
    }

    /**
     * Validate that all references are present in the classCache.
     */
    private void validateReferences(ClassCache classCache, Collection<Reference> references) throws IOException {
        for (Reference reference : references) {
            byte[] bytes = classCache.getClassResource(reference.className);
            if (null == bytes) {
                violations.add(new ReferenceViolation(WeaveViolationType.MISSING_ORIGINAL_BYTECODE,
                        reference.referenceOrigin, reference.className, "Could not find resource"));
            } else {
                ClassNode classNode = WeaveUtils.convertToClassNode(bytes);
                violations.addAll(reference.validateClassNode(classCache, classNode));
            }
        }
    }

    /**
     * Process the weave classes and add them to the result map.
     */
    private void processMatches(ClassCache classCache, Map<String, ClassNode> classNameToWeaveNode,
            Map<String, PreparedMatch> results, boolean isBaseMatch, ClassNode errorHandler) throws IOException {
        for (String weaveClassName : classNameToWeaveNode.keySet()) {
            byte[] originalBytes = classCache.getClassResource(weaveClassName);
            if (null == originalBytes) {
                violations.add(new WeaveViolation(WeaveViolationType.MISSING_ORIGINAL_BYTECODE, weaveClassName));
            } else {
                final ClassNode originalClassNode = WeaveUtils.convertToClassNode(originalBytes);
                final ClassNode weaveNode = classNameToWeaveNode.get(weaveClassName);
                final Set<String> requiredClassAnnotations = weavePackage.getRequiredAnnotationClassesForMethodAnnotationWeave(
                        weaveClassName);
                final Set<String> requiredMethodAnnotations =
                        weavePackage.getRequiredAnnotationClassesForMethodAnnotationWeave(weaveClassName);

                buildResults(classCache, originalClassNode, weaveClassName, weaveNode, results, isBaseMatch,
                        requiredClassAnnotations, requiredMethodAnnotations, errorHandler,
                        Collections.<String, byte[]>emptyMap());
            }
        }
    }

    private boolean buildResults(ClassCache classCache, ClassNode originalClassNode, String weaveClassName,
            ClassNode weaveNode, Map<String, PreparedMatch> results, boolean isBaseMatch,
            Set<String> requiredClassAnnotations, Set<String> requiredMethodAnnotations, ClassNode errorHandler,
            Map<String, byte[]> annotationProxyClasses) throws IOException {
        ClassMatch match = ClassMatch.match(originalClassNode, weaveNode, isBaseMatch, requiredClassAnnotations,
                requiredMethodAnnotations, classCache);
        NewFieldValidator.validate(match, violations);

        // exit sooner to prevent further processing (avoids NPE when no default constructor exists)
        if (match.isFatalWeaveViolation()) {
            violations.addAll(match.getViolations());
        }

        PreparedMatch prepared = PreparedMatch.prepare(match, errorHandler,
                this.getWeavePackage().getExtensionTemplate(), true);
        results.put(weaveClassName, prepared);
        if (null != prepared.getExtension()) {
            ClassNode extension = prepared.getExtension().generateExtensionClass();
            utilClasses.put(extension.name, extension);
        }
        for (String newInnerClassName : prepared.getNewInnerClasses()) {
            if (utilClasses.containsKey(newInnerClassName)) {
                ClassNode newInnerClassNode = utilClasses.get(newInnerClassName);
                match.validateNewInnerClass(newInnerClassNode);
                newInnerClassNode = prepared.prepareNewInnerClass(newInnerClassNode);
                utilClasses.remove(newInnerClassName);
                String newInnerClassRenamed = prepared.nameNewInnerClass(newInnerClassName);
                utilClasses.put(newInnerClassRenamed, newInnerClassNode);
            }
        }
        for (Map.Entry<String, ClassNode> annotationProxyClass : prepared.getAnnotationProxyClasses().entrySet()) {
            annotationProxyClasses.put(annotationProxyClass.getKey(),
                    WeaveUtils.convertToClassBytes(annotationProxyClass.getValue(), classCache));
        }

        violations.addAll((this.weaveViolationFilter == null ? match.getViolations() : this.weaveViolationFilter.filterViolationCollection(match.getViolations())));

        //Return true if this match had violations, even if they were filtered out of the PackageValidationResult violation list
        return match.getViolations().size() > 0;
    }

    /**
     * Rewrite all weave+util bytes to replace ops on newfields with extension class operations. See
     * {@link MethodProcessors#rewriteNewFieldCalls(String, Map, Set, Set, List, List)} for details.
     */
    private void rewriteAllNewFieldCalls() {
        List<PreparedExtension> preparedExtensions = new ArrayList<>();
        Collection<PreparedMatch> allMatches = new HashSet<>(exactMatches.size() + baseMatches.size());
        allMatches.addAll(exactMatches.values());
        allMatches.addAll(baseMatches.values());
        for (PreparedMatch pmatch : allMatches) {
            if (null != pmatch.getExtension()) {
                preparedExtensions.add(pmatch.getExtension());
            }
        }
        if (preparedExtensions.size() > 0) {
            // rewrite weaves
            for (PreparedMatch pmatch : allMatches) {
                MethodProcessors.rewriteNewFieldCalls(pmatch.getWeaveName(), pmatch.getPreparedMatchedMethods(),
                        pmatch.getNewFields(), pmatch.getMatchedFields(), preparedExtensions, getSuperWeaves(
                                pmatch.getWeaveSuperName()));
            }
            // rewrite util classes
            for (Map.Entry<String, ClassNode> entry : utilClasses.entrySet()) {
                ClassNode utilNode = entry.getValue();
                Set<String> matchedFields;
                if (null == utilNode.fields) {
                    matchedFields = new HashSet<>(0);
                } else {
                    matchedFields = new HashSet<>(utilNode.fields.size());
                    for (FieldNode field : utilNode.fields) {
                        matchedFields.add(field.name);
                    }
                }
                Map<Method, MethodNode> methodMap;
                if (null == utilNode.methods) {
                    methodMap = new HashMap<>(0);
                } else {
                    methodMap = new HashMap<>(utilNode.methods.size());
                    for (MethodNode methodNode : utilNode.methods) {
                        methodMap.put(new Method(methodNode.name, methodNode.desc), methodNode);
                    }
                }
                MethodProcessors.rewriteNewFieldCalls(utilNode.name, methodMap, new HashSet<String>(0), matchedFields,
                        preparedExtensions, getSuperWeaves(utilNode.superName));
                utilNode.methods = new ArrayList<>(methodMap.values());
            }
        }
    }

    private List<PreparedMatch> getSuperWeaves(String superName) {
        List<PreparedMatch> superMatches = new ArrayList<>();
        String currentName = superName;
        while (null != currentName) {
            PreparedMatch superMatch = exactMatches.get(currentName);
            if (null == superMatch) {
                break;
            }
            superMatches.add(superMatch);
            currentName = superMatch.getWeaveSuperName();
        }
        currentName = superName;
        while (null != currentName) {
            PreparedMatch superMatch = baseMatches.get(currentName);
            if (null == superMatch) {
                break;
            }
            superMatches.add(superMatch);
            currentName = superMatch.getWeaveSuperName();
        }
        return superMatches;
    }

    /**
     * @return true if the validation succeeded
     */
    public boolean succeeded() {
        return violations.size() == 0;
    }

    /**
     * @return a list of {@link WeaveViolation}s which were encountered while validating.
     */
    public List<WeaveViolation> getViolations() {
        return new ArrayList<>(violations);
    }

    /**
     * Weave the target class and return a {@link PackageWeaveResult}.
     */
    public PackageWeaveResult weave(String className, String[] superNames, String[] interfaceNames,
            byte[] targetBytes, ClassCache cache, Map<Method, Collection<String>> skipMethods) {
        ClassNode composite = WeaveUtils.convertToClassNode(targetBytes);
        return weave(className, superNames, interfaceNames, composite, cache, skipMethods);
    }

    /**
     * Weave the target class and return a {@link PackageWeaveResult}.
     */
    public PackageWeaveResult weave(String className, String[] superNames, String[] interfaceNames,
            ClassNode targetNode, ClassCache cache, Map<Method, Collection<String>> skipMethods) {
        ClassNode composite = targetNode;
        final Map<String, List<Method>> weavedMethods = new HashMap<>();

        // Locks during weaving are due to the non-thread safe nature of {@link ClassNode}.

        // first apply exact matches
        {
            PreparedMatch exactMatch = exactMatches.get(className);
            if (null != exactMatch) {
                ClassWeave classWeave;
                classWeave = ClassWeave.weave(exactMatch, composite, weavePackage, skipMethods);
                composite = classWeave.getComposite();
                final String key = exactMatch.getOriginalName();
                if (weavedMethods.containsKey(key)) {
                    weavedMethods.get(key).addAll(classWeave.getWeavedMethods());
                } else {
                    weavedMethods.put(key, classWeave.getWeavedMethods());
                }
            }
        }
        {
            PreparedMatch exactMatch = baseMatches.get(className); // matcher for abstract class
            if (null != exactMatch) {
                ClassWeave classWeave;
                classWeave = ClassWeave.weave(exactMatch, composite, weavePackage, skipMethods);
                composite = classWeave.getComposite();
                final String key = exactMatch.getOriginalName();
                if (weavedMethods.containsKey(key)) {
                    weavedMethods.get(key).addAll(classWeave.getWeavedMethods());
                } else {
                    weavedMethods.put(key, classWeave.getWeavedMethods());
                }
            }
        }

        // then apply super classes
        for (int i = 0; i < superNames.length; ++i) {
            PreparedMatch baseMatch = baseMatches.get(superNames[i]);
            if (null != baseMatch) {
                ClassWeave classWeave;
                classWeave = ClassWeave.weave(baseMatch, composite, weavePackage, skipMethods);
                composite = classWeave.getComposite();
                final String key = baseMatch.getOriginalName();
                if (weavedMethods.containsKey(key)) {
                    weavedMethods.get(key).addAll(classWeave.getWeavedMethods());
                } else {
                    weavedMethods.put(key, classWeave.getWeavedMethods());
                }
            }
        }

        // then apply interfaces
        for (int i = 0; i < interfaceNames.length; ++i) {
            PreparedMatch baseMatch = baseMatches.get(interfaceNames[i]);
            if (null != baseMatch) {
                ClassWeave classWeave;
                classWeave = ClassWeave.weave(baseMatch, composite, weavePackage, skipMethods);
                composite = classWeave.getComposite();
                final String key = baseMatch.getOriginalName();
                if (weavedMethods.containsKey(key)) {
                    weavedMethods.get(key).addAll(classWeave.getWeavedMethods());
                } else {
                    weavedMethods.put(key, classWeave.getWeavedMethods());
                }
            }
        }

        final Map<String, byte[]> annotationProxyClasses = new HashMap<>();
        // class annotation matches
        if (!allAnnotationClasses.isEmpty()) {
            Set<String> targetAnnotationsClasses = getAnnotationClasses(targetNode);
            Set<String> targetInterfacesAnnotationClasses = Collections.emptySet();
            if (!baseAnnotationClasses.isEmpty()) {
                targetInterfacesAnnotationClasses = getAllInterfaceAnnotationClasses(targetNode, cache);
            }

            // check for exact annotation matches
            for (Map.Entry<String, ClassNode> entry : allAnnotationClasses.entrySet()) {
                if (targetAnnotationsClasses.contains(entry.getKey())) {
                    composite = getAnnotationMatchComposite(targetNode, entry.getValue(), composite, weavedMethods,
                            cache, annotationProxyClasses, skipMethods);
                }
            }

            // check for base annotation matches
            for (Map.Entry<String, ClassNode> entry : baseAnnotationClasses.entrySet()) {
                if (targetInterfacesAnnotationClasses.contains(entry.getKey())) {
                    composite = getAnnotationMatchComposite(targetNode, entry.getValue(), composite, weavedMethods,
                            cache, annotationProxyClasses, skipMethods);
                }
            }
        }

        // method annotation matches
        if (!allMethodAnnotationClasses.isEmpty()) {
            Set<String> targetMethodAnnotationsClasses = getMethodAnnotationClasses(targetNode);

            for (Map.Entry<String, ClassNode> entry : allMethodAnnotationClasses.entrySet()) {
                if (targetMethodAnnotationsClasses.contains(entry.getKey())) {
                    composite = getAnnotationMatchComposite(targetNode, entry.getValue(), composite, weavedMethods,
                            cache, annotationProxyClasses, skipMethods);

                    // After we find a match we can break out of the loop to prevent weaving a method multiple times
                    // due to having multiple matching annotations that have the same underlying weave code.
                    break;
                }
            }
        }

        if (weavePackage != null) {
            // Run any postprocessors that were passed in. It's important that we do this before the inliner runs below
            ClassNode result = new SynchronizedClassNode(WeaveUtils.ASM_API_LEVEL);
            ClassVisitor postprocessChain = weavePackage.getConfig().getPostprocessor().postprocess(className, result,
                    Collections.<String>emptySet(), weavePackage, false);
            composite.accept(postprocessChain);
            composite = result;
        }

        return new PackageWeaveResult(this, className, composite, weavedMethods, annotationProxyClasses);
    }

    private ClassNode getAnnotationMatchComposite(ClassNode targetNode,
                                                  ClassNode weaveNode,
                                                  ClassNode composite,
                                                  Map<String, List<Method>> weavedMethods,
                                                  ClassCache cache,
                                                  Map<String, byte[]> annotationProxyClasses,
                                                  Map<Method, Collection<String>> skipMethods) {

        try {
            boolean isInterfaceMatch = WeaveUtils.isWeaveWithAnnotationInterfaceMatch(weaveNode);
            Map<String, PreparedMatch> results = new HashMap<>();
            boolean targetNodeContainsViolations = buildResults(cache, targetNode, weaveNode.name, weaveNode, results, isInterfaceMatch,
                    weavePackage.getRequiredAnnotationClassesForAnnotationWeave(weaveNode.name),
                    weavePackage.getRequiredAnnotationClassesForMethodAnnotationWeave(weaveNode.name),
                    errorHandler, annotationProxyClasses);

            if (targetNodeContainsViolations) {
                return composite;
            }

            for (Map.Entry<String, PreparedMatch> result : results.entrySet()) {
                PreparedMatch prepared = result.getValue();
                if (prepared != null) {
                    ClassWeave classWeave = ClassWeave.weave(prepared, composite, weavePackage, skipMethods);
                    composite = classWeave.getComposite();

                    // Key is only used for logging at the moment.
                    final String key = prepared.getWeaveName();
                    if (weavedMethods.containsKey(key)) {
                        weavedMethods.get(key).addAll(classWeave.getWeavedMethods());
                    } else {
                        weavedMethods.put(key, classWeave.getWeavedMethods());
                    }

                }
            }
        } catch (Exception ignored) {
        }

        return composite;
    }

    /**
     * Returns the set of annotation class names that exist on all direct interfaces of the target class
     */
    private Set<String> getAllInterfaceAnnotationClasses(ClassNode targetClass, ClassCache classCache) {
        Set<String> interfaceAnnotationClasses = new HashSet<>();

        try {
            ClassInformation classInformation = classCache.getClassInformation(targetClass.name);
            if (classInformation != null) {
                Set<String> interfaceNames = classInformation.getAllInterfaces(classCache);
                if (interfaceNames != null) {
                    for (String interfaceName : interfaceNames) {
                        ClassInformation interfaceInformation = classCache.getClassInformation(interfaceName);
                        if (interfaceInformation != null) {
                            interfaceAnnotationClasses.addAll(interfaceInformation.classAnnotationNames);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return interfaceAnnotationClasses;
    }

    private Set<String> getAnnotationClasses(ClassNode targetNode) {
        Set<String> classAnnotations = new HashSet<>();
        if (targetNode.visibleAnnotations != null) {
            for (AnnotationNode visibleAnnotation : targetNode.visibleAnnotations) {
                classAnnotations.add(Type.getType(visibleAnnotation.desc).getClassName());
            }
        }

        if (targetNode.invisibleAnnotations != null) {
            for (AnnotationNode invisibleAnnotation : targetNode.invisibleAnnotations) {
                classAnnotations.add(Type.getType(invisibleAnnotation.desc).getClassName());
            }
        }
        return classAnnotations;
    }

    private Set<String> getMethodAnnotationClasses(ClassNode targetNode) {
        Set<String> methodAnnotations = new HashSet<>();
        for (MethodNode methodNode : targetNode.methods) {
            if (methodNode.visibleAnnotations != null) {
                for (AnnotationNode visibleAnnotation : methodNode.visibleAnnotations) {
                    methodAnnotations.add(Type.getType(visibleAnnotation.desc).getClassName());
                }
            }

            if (methodNode.invisibleAnnotations != null) {
                for (AnnotationNode invisibleAnnotation : methodNode.invisibleAnnotations) {
                    methodAnnotations.add(Type.getType(invisibleAnnotation.desc).getClassName());
                }
            }
        }
        return methodAnnotations;
    }

    /**
     * Compute and return a map of (name -> bytes) for all utility classes from this validation result.
     *
     * @see NewClassAppender
     */
    public Map<String, byte[]> computeUtilityClassBytes(ClassCache cache) {
        Map<String, byte[]> utilClassBytes = new HashMap<>(utilClasses.size());
        for (Map.Entry<String, ClassNode> entry : utilClasses.entrySet()) {
            // Run post-processors on our utility classes to ensure that we capture API supportability metrics properly
            ClassNode result = new SynchronizedClassNode(WeaveUtils.ASM_API_LEVEL);
            ClassVisitor postprocessChain = weavePackage.getConfig().getPostprocessor().postprocess(entry.getValue().name, result,
                    Collections.<String>emptySet(), weavePackage, true );
            entry.getValue().accept(postprocessChain);

            utilClassBytes.put(entry.getKey(), WeaveUtils.convertToClassBytes(result, cache));
        }
        return utilClassBytes;
    }

    /**
     * @return the WeavePackage this validation came from.
     */
    public WeavePackage getWeavePackage() {
        return this.weavePackage;
    }

    private static WeavePackageConfig weavePackageConfig(PackageValidationResult p) {
        return p.getWeavePackage().getConfig();
    }
}
