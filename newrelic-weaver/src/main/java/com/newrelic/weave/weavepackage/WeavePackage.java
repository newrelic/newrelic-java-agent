/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.WeaveWithAnnotation;
import com.newrelic.weave.MethodProcessors;
import com.newrelic.weave.utils.BootstrapLoader;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassInformation;
import com.newrelic.weave.utils.ReferenceUtils;
import com.newrelic.weave.utils.Streams;
import com.newrelic.weave.utils.SynchronizedClassNode;
import com.newrelic.weave.utils.WeaveClassInfo;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.weavepackage.language.LanguageAdapter;
import com.newrelic.weave.weavepackage.language.LanguageAdapterResult;
import com.newrelic.weave.weavepackage.language.RegisteredLanguageAdapters;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static com.newrelic.api.agent.weaver.MatchType.Interface;

/**
 * A WeavePackage does the following:
 * <ul>
 * <li>Stores the weave bytes from the weave module. Separates bytes into exact and base matches.</li>
 * <li>Stores the utility class bytes (non-weaved) from the weave module.</li>
 * </ul>
 * 
 * For the agent, there is one WeavePackage per instrumentation module.
 */
public class WeavePackage {

    /**
     * Create a WeavePackage from a jar source.
     *
     * @param jarStream steam of a JAR file
     * @param config {@link WeavePackageConfig} containing metadata about the package
     * @return package containing
     */
    public static WeavePackage createWeavePackage(JarInputStream jarStream, WeavePackageConfig config) throws Exception {
        List<byte[]> classBytes = new ArrayList<>();
        JarEntry entry = null;
        while ((entry = jarStream.getNextJarEntry()) != null) {
            if (entry.getName().endsWith(".class")) {
                classBytes.add(Streams.read(jarStream, false));
            }
        }
        return new WeavePackage(config, classBytes);
    }

    // the weaves maps store the source bytes for weave classes
    private final Map<String, ClassNode> exactWeaves = new ConcurrentHashMap<>();
    private final Map<String, ClassNode> baseWeaves = new ConcurrentHashMap<>();
    private final Map<String, ClassNode> utilClasses = new ConcurrentHashMap<>();
    private final Map<String, MatchType> weaveMatches = new ConcurrentHashMap<>();

    /**
     * This is to support matches that have no name. That is, {@link WeaveWithAnnotation} with no @Weave(originalName)
     */
    private final Map<String, ClassNode> allClassAnnotationWeaves = new ConcurrentHashMap<>();
    private final Map<String, ClassNode> allMethodAnnotationWeaves = new ConcurrentHashMap<>();
    private final Map<String, ClassNode> baseAnnotationWeaves = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> requiredClassAnnotationsLookup = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> requiredMethodAnnotationsLookup = new ConcurrentHashMap<>();

    private final Map<String, Reference> references = new ConcurrentHashMap<>();
    private final Set<String> skipIfPresentClasses = Sets.newConcurrentHashSet();
    private final Set<String> methodSignatures = Sets.newConcurrentHashSet();
    private final Queue<WeaveViolation> packageViolations = Queues.newConcurrentLinkedQueue();
    private final Map<String, String> renames = new ConcurrentHashMap<>();
    private final WeavePackageConfig config;
    private volatile boolean weavesBootstrap = false;
    private ClassNode errorHandler;
    private ClassNode extensionTemplate;

    /**
     * Create a WeavePackage from a list of weave class bytes.
     * @param config {@link WeavePackageConfig} containing metadata about the package
     * @param weavePackageBytes list of all class bytes in the package
     */
    public WeavePackage(WeavePackageConfig config, List<byte[]> weavePackageBytes) {
        this.config = config;
        processWeaveBytes(weavePackageBytes);
    }

    /**
     * Processes all classes in the package.  Part of package initialization.
     * @param weavePackageBytes list of all class bytes in the package
     * @return list of weave violations found during package initialization
     */
    protected final List<WeaveViolation> processWeaveBytes(List<byte[]> weavePackageBytes) {
        List<WeaveViolation> violations = new ArrayList<>();
        for (LanguageAdapter adapter : RegisteredLanguageAdapters.getLanguageAdapters()) {
            try {
                LanguageAdapterResult result = adapter.adapt(weavePackageBytes);
                weavePackageBytes = result.getAdaptedBytes();
                violations.addAll(result.getViolations());
            } catch (Throwable ignored) {
            }
        }
        // read weave annotations on each class bytes and put in appropriate map
        for (byte[] weaveClassBytes : weavePackageBytes) {
            ClassNode weaveNode = WeaveUtils.convertToClassNode(weaveClassBytes);
            WeaveClassInfo weave = new WeaveClassInfo(weaveNode);
            violations.addAll(weave.getViolations());

            final boolean isClassAnnotationMatch = !weave.getRequiredClassAnnotations().isEmpty();
            final boolean isMethodAnnotationMatch = !weave.getRequiredMethodAnnotations().isEmpty();
            if (isClassAnnotationMatch || isMethodAnnotationMatch) {
                if (isClassAnnotationMatch) {
                    for (String requiredAnnotation : weave.getRequiredClassAnnotations()) {
                        if (weave.getMatchType().equals(Interface)) {
                            baseAnnotationWeaves.put(requiredAnnotation, weaveNode);
                        }
                        allClassAnnotationWeaves.put(requiredAnnotation, weaveNode);
                    }
                    requiredClassAnnotationsLookup.put(weaveNode.name, weave.getRequiredClassAnnotations());
                }
                if (isMethodAnnotationMatch) {
                    for (String requiredAnnotation : weave.getRequiredMethodAnnotations()) {
                        if (!isClassAnnotationMatch) {
                            allMethodAnnotationWeaves.put(requiredAnnotation, weaveNode);
                        } else if (!allMethodAnnotationWeaves.containsKey(requiredAnnotation)) {
                            allMethodAnnotationWeaves.put(requiredAnnotation, weaveNode);
                        }
                    }
                    requiredMethodAnnotationsLookup.put(weaveNode.name, weave.getRequiredMethodAnnotations());
                }
            } else if (null == weave.getMatchType()) {
                if (weave.isSkipIfPresent()) {
                    skipIfPresentClasses.add(weave.getOriginalName());
                } else {
                    utilClasses.put(weaveNode.name, weaveNode);
                }
            } else {
                if (!weaveNode.name.equals(weave.getOriginalName())) {
                    renames.put(weaveNode.name, weave.getOriginalName());
                }
                // check if added and merge
                weaveMatches.put(weave.getOriginalName(), weave.getMatchType());

                switch (weave.getMatchType()) {
                case BaseClass:
                case Interface:
                    baseWeaves.put(weave.getOriginalName(), weaveNode);
                    break;
                case ExactClass:
                default:
                    exactWeaves.put(weave.getOriginalName(), weaveNode);
                    break;
                }
            }
        }

        // preprocess
        this.preprocessAllWeaveCode();

        this.packageViolations.addAll(violations);
        if (isBootstrapClassName(this.exactWeaves.keySet()) || isBootstrapClassName(this.baseWeaves.keySet())) {
            this.weavesBootstrap = true; // FIXME this isn't getting set
        }
        return violations;
    }

    /**
     * Indicates whether any of the specified class names are loaded on the bootstrap.
     * @param names collection of class names
     * @return <code>true</code> if any of the classes are loaded on the bootstrap
     */
    public boolean isBootstrapClassName(Collection<String> names) {
        for (String name : names) {
            if (BootstrapLoader.get().isBootstrapClass(name)) {
                return true; // FIXME should this return true for Context?
            }
        }
        return false;
    }

    /**
     * Runs the pre-processor on all of the classes in the package.  Part of package initialization.
     */
    private void preprocessAllWeaveCode() {
        // AgentPreprocessors may change the name of utility classes.
        Set<String> renamedUtilityClassesToRemove = new HashSet<>();

        for (ClassNode node : this.utilClasses.values()) {
            String nameBefore = node.name;
            node = preprocess(node);
            utilClasses.put(node.name, node);

            if (!node.name.equals(nameBefore)) {
                renamedUtilityClassesToRemove.add(nameBefore);
            }
        }
        for (Entry<String, ClassNode> entry : this.exactWeaves.entrySet()) {
            ClassNode node = preprocess(entry.getValue());
            entry.setValue(node);
        }
        for (Entry<String, ClassNode> entry : this.baseWeaves.entrySet()) {
            ClassNode node = preprocess(entry.getValue());
            entry.setValue(node);
        }
        for (Entry<String, ClassNode> entry : this.allClassAnnotationWeaves.entrySet()) {
            ClassNode node = preprocess(entry.getValue());
            entry.setValue(node);
        }
        for (Entry<String, ClassNode> entry : this.allMethodAnnotationWeaves.entrySet()) {
            ClassNode node = preprocess(entry.getValue());
            entry.setValue(node);
        }
        for (Entry<String, ClassNode> entry : this.baseAnnotationWeaves.entrySet()) {
            ClassNode node = preprocess(entry.getValue());
            entry.setValue(node);
        }

        this.errorHandler = preprocess(config.getErrorHandleClassNode());
        this.extensionTemplate = preprocess(config.getExtensionTemplate());

        for (String renamedUtilityClass : renamedUtilityClassesToRemove) {
            utilClasses.remove(renamedUtilityClass);
        }

        // now that all the preprocessing is done, we can create our references
        for (ClassNode node : this.utilClasses.values()) {
            checkReferences(node);
        }

        for (Entry<String, ClassNode> entry : this.exactWeaves.entrySet()) {
            checkReferences(entry.getValue());

            for (MethodNode methodNode : entry.getValue().methods) {
                if (WeaveUtils.isEmptyConstructor(methodNode)) {
                    // Generated empty constructor
                    continue;
                }

                methodSignatures.add(methodNode.name + methodNode.desc);
            }
        }

        for (Entry<String, ClassNode> entry : this.baseWeaves.entrySet()) {
            checkReferences(entry.getValue());

            for (MethodNode methodNode : entry.getValue().methods) {
                if (WeaveUtils.isEmptyConstructor(methodNode)) {
                    // Generated empty constructor
                    continue;
                }
                methodSignatures.add(methodNode.name + methodNode.desc);
            }
        }

    }

    /**
     * Find types/fields/methods referenced by the specified class
     * 
     * @param node ClassNode to check
     */
    private void checkReferences(ClassNode node) {
        Set<Reference> referencedClasses = Reference.create(node);
        for (Reference reference : referencedClasses) {
            if (reference.className.startsWith("java/") || reference.className.startsWith("javax/")
                    || reference.className.startsWith("com/newrelic/api/")
                    || reference.className.startsWith("com/newrelic/agent/")
                    || utilClasses.containsKey(reference.className) || exactWeaves.containsKey(reference.className)
                    || baseWeaves.containsKey(reference.className)) {
                // compiler already validated most of these references

                continue;
            }
            if (this.references.containsKey(reference.className)) {
                Reference otherReference = this.references.get(reference.className);
                otherReference.merge(reference);
            } else {
                this.references.put(reference.className, reference);
            }
        }
    }

    /**
     * Run the pre-processor on a specific class in the package. Part of package initialization.
     * 
     * @param input class to process
     * @return processed class
     */
    private ClassNode preprocess(ClassNode input) {
        ClassNode result = new SynchronizedClassNode(WeaveUtils.ASM_API_LEVEL);

        // user configured preprocessors go first
        ClassVisitor preprocessChain = config.getPreprocessor().preprocess(result, utilClasses.keySet(), this);

        preprocessChain = MethodProcessors.fixInvocationInstructions(preprocessChain, weaveMatches);
        preprocessChain = MethodProcessors.replaceGetImplementationTitle(preprocessChain, this.getName());
        if (renames.size() > 0) {
            preprocessChain = ReferenceUtils.getRenamingVisitor(renames, preprocessChain);
        }

        input.accept(preprocessChain);

        return result;
    }

    /**
     * Returns true if this package contains at least one matcher for a class.
     */
    public boolean hasMatcher(String className, String[] superNames, String[] interfaceNames,
            Set<String> classAnnotations, Set<String> methodAnnotations, ClassCache classCache) throws IOException {
        if (this.exactWeaves.containsKey(className) || this.baseWeaves.containsKey(className))
            return true;
        for (int i = 0; i < superNames.length; ++i) {
            if (this.baseWeaves.containsKey(superNames[i]))
                return true;
        }
        for (int i = 0; i < interfaceNames.length; ++i) {
            if (this.baseWeaves.containsKey(interfaceNames[i]))
                return true;
        }

        // Check to see if any of the annotations on the class exist in the set of required Exact match annotations
        Set<String> matchingAnnotations = Sets.intersection(allClassAnnotationWeaves.keySet(), classAnnotations);
        if (!matchingAnnotations.isEmpty()) {
            return true;
        }

        // If we have any base/interface annotation weaves check to see if the interfaces have the correct annotation(s)
        if (!baseAnnotationWeaves.isEmpty()) {
            Set<String> requiredBaseAnnotations = baseAnnotationWeaves.keySet();
            for (String interfaceName : interfaceNames) {
                ClassInformation classInformation = classCache.getClassInformation(interfaceName);
                if (classHasRequiredAnnotations(classInformation, requiredBaseAnnotations)) {
                    return true;
                }
            }
        }

        // Check to see if any of the annotations on the methods exist in the set of required method annotations
        Set<String> matchingMethodAnnotations = Sets.intersection(allMethodAnnotationWeaves.keySet(), methodAnnotations);
        if (!matchingMethodAnnotations.isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if the provided class (represented by ClassInformation) has any of the provided requiredAnnotations
     * 
     * @param classInformation the class to look for the annotations on
     * @param requiredAnnotations the required annotations to look for
     * @return true if the provided class has at least one of the required annotations, false otherwise
     */
    private boolean classHasRequiredAnnotations(ClassInformation classInformation, Set<String> requiredAnnotations) {
        return classInformation != null &&
                !Sets.intersection(classInformation.classAnnotationNames, requiredAnnotations).isEmpty();

    }

    /**
     * Get violations present at the package level. Violations here means that this weave package will not validate
     * against any classloader.
     * 
     * @return a list of {@link WeaveViolation}s
     */
    public List<WeaveViolation> getPackageViolations() {
        return new ArrayList<>(packageViolations);
    }

    /**
     * Validate this package using the specified {@link ClassCache} for finding class metadata.
     *
     * @param cache {@link ClassCache} for finding class metadata
     * @return result of validation
     * @throws IOException
     */
    public PackageValidationResult validate(ClassCache cache) throws IOException {
        if (packageViolations.size() == 0) {
            return new PackageValidationResult(this, cache, references.values(), exactWeaves, baseWeaves,
                    allClassAnnotationWeaves, baseAnnotationWeaves, allMethodAnnotationWeaves, utilClasses,
                    skipIfPresentClasses, errorHandler, extensionTemplate);
        } else {
            // if the package already has violations it will fail against every classloader. Return a failed result.
            return new PackageValidationResult(this, packageViolations);
        }
    }

    /**
     * Metadata about this package.
     */
    public WeavePackageConfig getConfig() {
        return config;
    }

    /**
     * Name of this package.
     */
    public String getName() {
        return config.getName();
    }

    /**
     * Version of this package.
     */
    public float getVersion() {
        return config.getVersion();
    }

    /**
     * Map of class name to match type, for all classes with a {@link com.newrelic.api.agent.weaver.Weave} annotation.
     */
    public Map<String, MatchType> getMatchTypes() {
        return this.weaveMatches;
    }

    /**
     * Names of classes that are referenced by classes in this package.
     */
    public Set<String> getReferencedClassNames() {
        return references.keySet();
    }

    public Map<String, String> getRenames() {
        return renames;
    }

    /**
     * All the annotations specified in all {@link WeaveWithAnnotation#annotationClasses()} in this package.
     */
    public Set<String> getAllRequiredAnnotationClasses() {
        return allClassAnnotationWeaves.keySet();
    }

    public Set<String> getAllRequiredMethodAnnotationClasses() {
        return allMethodAnnotationWeaves.keySet();
    }

    // package private for testing
    Map<String,ClassNode> getAllClassAnnotationWeaves() {
        return allClassAnnotationWeaves;
    }

    // package private for testing
    Map<String,ClassNode> getAllMethodAnnotationWeaves() {
        return allMethodAnnotationWeaves;
    }

    /**
     * Returns the set of required annotation classes listed on the provided annotation weave class
     * 
     * @param annotationWeaveClassName the name of the annotation weave class
     * @return the set of required class annotations for this annotation weave
     */
    public Set<String> getRequiredAnnotationClassesForAnnotationWeave(String annotationWeaveClassName) {
        final Set<String> requiredAnnotations = requiredClassAnnotationsLookup.get(annotationWeaveClassName);
        return requiredAnnotations != null ? requiredAnnotations : Collections.<String>emptySet();
    }

    public Set<String> getRequiredAnnotationClassesForMethodAnnotationWeave(String annotationWeaveClassName) {
        final Set<String> requiredAnnotations = requiredMethodAnnotationsLookup.get(annotationWeaveClassName);
        return requiredAnnotations != null ? requiredAnnotations : Collections.<String>emptySet();
    }

    /**
     * Class names that are required to exist on the classpath for this package to be valid.
     */
    public Set<String> getRequiredClasses() {
        Set<String> required = new HashSet<>();
        required.addAll(exactWeaves.keySet());
        required.addAll(baseWeaves.keySet());
        required.addAll(references.keySet());
        required.addAll(allClassAnnotationWeaves.keySet());
        required.addAll(allMethodAnnotationWeaves.keySet());
        return required;
    }

    /**
     * Class names that cannot exist on the classpath for this package to be valid.
     */
    public Set<String> getIllegalClasses() {
        return skipIfPresentClasses;
    }

    /**
     * @return true if this weave package tries to weave into a class on the bootstrap classloader.
     */
    public boolean weavesBootstrap() {
        return this.weavesBootstrap;
    }

    public Set<String> getMethodSignatures() {
        return methodSignatures;
    }

    @Override
    public String toString() {
        return "WeavePackage [config=" + config + "]";
    }

    Map<String, ClassNode> getExactWeaves() {
        return exactWeaves;
    }

    Map<String, ClassNode> getBaseWeaves() {
        return baseWeaves;
    }

    Map<String, ClassNode> getUtilClasses() {
        return utilClasses;
    }

    Map<String, Reference> getReferences() {
        return references;
    }

    void setWeavesBootstrap() {
        this.weavesBootstrap = true;
    }

    public ClassNode getExtensionTemplate() {
        return extensionTemplate;
    }
}
