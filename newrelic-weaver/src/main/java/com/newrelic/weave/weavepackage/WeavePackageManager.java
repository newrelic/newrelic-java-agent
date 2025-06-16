/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Sets;
import com.newrelic.weave.utils.BootstrapLoader;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassInformation;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Manages a group of {@link WeavePackage}s. This class is thread safe.
 */
public class WeavePackageManager {
    /**
     * WeavePackageName -> WeavePackage
     */
    private final ConcurrentMap<String, WeavePackage> weavePackages = new ConcurrentHashMap<>();

    /**
     * The set of @Weave and reference classes in all registered weave packages (used as an optimization)
     */
    private final Set<String> requiredClasses = Sets.newConcurrentHashSet();

    /**
     * The set of all method signatures in all registered @Weave classes (used as an optimization)
     */
    private final Set<String> methodSignatures = Sets.newConcurrentHashSet();

    /**
     * The set of all annotations required in all registered @Weave pacakges
     */
    private final Set<String> requiredAnnotationClasses = Sets.newConcurrentHashSet();

    private final Set<String> requiredMethodAnnotationClasses = Sets.newConcurrentHashSet();

    /**
     * ClassLoader -> (WeavePackageName -> WeavePackage)
     */
    private final Cache<ClassLoader, ConcurrentMap<String, WeavePackage>> optimizedWeavePackages = Caffeine.newBuilder().weakKeys().executor(Runnable::run).build();

    private final WeavePackageLifetimeListener packageListener;
    private final Instrumentation instrumentation;
    private final int maxPreValidatedClassLoaders;
    private final boolean preValidateWeavePackages;
    private final boolean preMatchWeaveMethods;

    /**
     * Stores the a PackageValidationResult for the current thread. This exists to work around an issue where a `defineClass()` call
     * will cause a weave package to be validated again, generating different utility classes and breaking the instrumentation module.
     *
     * By storing this in a thread local we are able to check and re-use the result in the secondary call without requiring the package
     * to be added to the map of `validPackages` which could cause a NoClassDefFoundError if we add the utility classes in before the
     * weave classes have a chance to get added.
     */
    private static final ThreadLocal<PackageValidationResult> currentValidationResult = new ThreadLocal<>();

    static final int MAX_VALID_PACKAGE_CACHE = 100;
    static final int MAX_INVALID_PACKAGE_CACHE = 100;

    /**
     * classloader -> (weave package -> result of successful weaving)
     */
    Cache<ClassLoader, ConcurrentMap<WeavePackage, PackageValidationResult>> validPackages = Caffeine.newBuilder().weakKeys().initialCapacity(
            8).maximumSize(MAX_VALID_PACKAGE_CACHE).executor(Runnable::run).build();
    /**
     * classloader -> (weave package -> result of successful weaving)
     */
    Cache<ClassLoader, ConcurrentMap<WeavePackage, PackageValidationResult>> invalidPackages = Caffeine.newBuilder().weakKeys().initialCapacity(
            8).maximumSize(MAX_INVALID_PACKAGE_CACHE).executor(Runnable::run).build();

    WeavePackageManager() {
        this(null);
    }

    /**
     * Create a manager with the specified listener
     *
     * @param listener callback interface that's invoked when packages are registered, deregistered, or validated
     */
    public WeavePackageManager(WeavePackageLifetimeListener listener) {
        this(listener, null, 10, true, true);
    }

    /**
     * Create a manager with the specified listener and {@link Instrumentation} instance.
     *
     * @param listener callback interface that's invoked when packages are registered, deregistered, or validated
     * @param instrumentation {@link Instrumentation} instance
     */
    public WeavePackageManager(WeavePackageLifetimeListener listener, Instrumentation instrumentation,
            int maxPreValidatedClassLoaders, boolean preValidateWeavePackages, boolean preMatchWeaveMethods) {
        this.packageListener = listener;
        this.instrumentation = instrumentation;
        this.maxPreValidatedClassLoaders = maxPreValidatedClassLoaders;
        this.preValidateWeavePackages = preValidateWeavePackages;
        this.preMatchWeaveMethods = preMatchWeaveMethods;
    }

    /**
     * Indicates whether this manager can weave classes loaded by the bootstrap class loader.
     *
     * @return <code>true</code> if bootstrap classes can be weaved
     */
    boolean canWeaveBootstrapClassLoader() {
        return null != instrumentation;
    }

    /**
     * Indicates whether or not the specified package is currently registered by this manager.
     *
     * @param weavePackageName package name
     * @return <code>true</code> if registered
     */
    public boolean isRegistered(String weavePackageName) {
        return weavePackages.containsKey(weavePackageName);
    }

    /**
     * Add the specified package to this manager
     *
     * @param weavePackage package to add
     */
    public void register(WeavePackage weavePackage) {
        if (null != weavePackage && (!weavePackages.containsKey(weavePackage.getName())) && (weavePackages.putIfAbsent(
                weavePackage.getName(), weavePackage) == null)) {
            methodSignatures.addAll(weavePackage.getMethodSignatures());
            requiredClasses.addAll(weavePackage.getRequiredClasses());
            requiredAnnotationClasses.addAll(weavePackage.getAllRequiredAnnotationClasses());
            requiredMethodAnnotationClasses.addAll(weavePackage.getAllRequiredMethodAnnotationClasses());

            if (null != packageListener) {
                packageListener.registered(weavePackage);
            }
            // Just in case we've already built up the cache before an extension was registered we
            // need to clear the cache to rebuild with the new extension jar on the next call to match()
            optimizedWeavePackages.invalidateAll();
        }
    }

    /**
     * Remove the specified weave package from this manager.
     *
     * @param weavePackage package to remove
     * @return removed package, or <code>null</code> if nothing was removed
     */
    public WeavePackage deregister(WeavePackage weavePackage) {
        if (null == weavePackage) {
            return null;
        }
        WeavePackage remove = weavePackages.remove(weavePackage.getName());
        if (null != remove) {
            optimizedWeavePackages.invalidateAll();
            requiredClasses.removeAll(remove.getRequiredClasses());
            // Rebuild method signatures from weavePackages map
            rebuildWeavePackages();
        }
        if (null != remove && null != packageListener) {
            packageListener.deregistered(remove);
        }
        return remove;
    }

    private void rebuildWeavePackages() {
        Set<String> updatedMethodSignatures = Sets.newConcurrentHashSet();
        for (WeavePackage weavePackage : weavePackages.values()) {
            updatedMethodSignatures.addAll(weavePackage.getMethodSignatures());
        }
        methodSignatures.clear();
        methodSignatures.addAll(updatedMethodSignatures);
    }

    /**
     * Remove the specified weave package from this manager.
     *
     * @param weavePackageName name of package to remove
     * @return removed package, or <code>null</code> if nothing was removed
     */
    public WeavePackage deregister(String weavePackageName) {
        return deregister(weavePackages.get(weavePackageName));
    }

    /**
     * Find the package for the specified name if registered by this manager.
     *
     * @param name package name
     * @return registered package, or <code>null</code> if nothing was registered
     */
    public WeavePackage getWeavePackage(String name) {
        return weavePackages.get(name);
    }

    /**
     * All of the packages registered by this class.
     */
    public Collection<WeavePackage> getRegisteredPackages() {
        return weavePackages.values();
    }

    /**
     * A hack to treat the bootsrap like a {@link ClassLoader} to clean up implementations.
     *
     * @param classloader instance of a {@link ClassLoader}, or <code>null</code> indicating the bootstrap
     * @return provided classloader instance, or {@link BootstrapLoader#PLACEHOLDER} if <code>null</code>
     */
    private ClassLoader classLoaderSub(ClassLoader classloader) {
        if (null == classloader) { // TODO check if OTel class and return their bootstrap classloader proxy??
            return BootstrapLoader.PLACEHOLDER;
        } else {
            return classloader;
        }
    }

    /**
     * Determine if the specified class and class loader match any packages.
     *
     * @param classloader classloader containing classpath to validate against
     * @param className name of the class
     * @param cache {@link ClassCache} used to resolve classes without loading them
     * @return validation result set
     */
    public Set<PackageValidationResult> match(ClassLoader classloader, String className, ClassCache cache)
            throws IOException {
        classloader = classLoaderSub(classloader);
        ClassInformation classInformation = cache.getClassInformation(className);
        if (null == classInformation) {
            return Collections.emptySet();
        }

        String[] superNames = classInformation.getAllSuperNames(cache).toArray(new String[0]);
        String[] interfaceNames = classInformation.getAllInterfaces(cache).toArray(new String[0]);
        Set<String> classAnnotations = classInformation.classAnnotationNames;
        Set<String> methodAnnotations = classInformation.methodAnnotationNames;

        return match(classloader, cache, className, classAnnotations, methodAnnotations, superNames, interfaceNames);
    }

    private Set<PackageValidationResult> match(ClassLoader classloader, ClassCache cache, String className,
            Set<String> classAnnotations, Set<String> methodAnnotations, String[] superNames, String[] interfaceNames)
            throws IOException {
        Set<PackageValidationResult> matchedPackageResults = Sets.newConcurrentHashSet();

        Map<String, WeavePackage> classloaderWeavePackages;
        if (preValidateWeavePackages && optimizedWeavePackages.asMap().size() < maxPreValidatedClassLoaders) {
            classloaderWeavePackages = getOptimizedWeavePackages(classloader, cache);
        } else {
            classloaderWeavePackages = weavePackages;
        }

        for (WeavePackage weavePackage : classloaderWeavePackages.values()) {
            if (weavePackage.hasMatcher(className, superNames, interfaceNames, classAnnotations, methodAnnotations, cache)) {
                PackageValidationResult successfulValidation = this.getSuccessfulValidation(className, superNames[0], interfaceNames, classloader, cache,
                        weavePackage);
                if (null != successfulValidation) {
                    matchedPackageResults.add(successfulValidation);
                }
            }
        }
        return matchedPackageResults;
    }

    /**
     * Weave all of the matched packages with the specified target bytes and return the composite class.
     *
     * @param classloader classloader to resolve classes with
     * @param className target class name
     * @param targetBytes target class bytes
     * @return composite class bytes, or <code>null</code> if no weaving occurred
     */
    public byte[] weave(ClassLoader classloader, String className, byte[] targetBytes,
                        Map<Method, Collection<String>> skipMethods) throws IOException {
        classloader = classLoaderSub(classloader);
        ClassCache cache = new ClassCache(new ClassLoaderFinder(classloader));
        return weave(classloader, cache, className, targetBytes, skipMethods, null);
    }

    /**
     * Weave all of the matched packages with the specified target bytes using the specified cache and listener.
     *
     * @param classloader classloader to resolve classes with
     * @param cache {@link ClassCache} to find class metadata
     * @param className target class name
     * @param targetBytes target class bytes
     * @param weaveListener listener containing callback if/when the composite is created
     * @return composite class bytes, or <code>null</code> if no weaving occurred
     */
    public byte[] weave(ClassLoader classloader, ClassCache cache, String className,
                        byte[] targetBytes, Map<Method, Collection<String>> skipMethods,
            ClassWeavedListener weaveListener) throws IOException {
        classloader = classLoaderSub(classloader);

        if (preMatchWeaveMethods && !containsPossibleClassOrMethodMatch(className, targetBytes, requiredClasses, methodSignatures, cache)) {
            // No potential method match was found, we are definitely not weaving this class so we should exit now
            return null;
        }

        ClassInformation classInformation = cache.getClassInformation(className);
        if (classInformation == null) { // fixme bombing out here
            return null;
        }

        String[] superNames = cache.getClassInformation(className).getAllSuperNames(cache).toArray(new String[0]);
        String[] interfaceNames = cache.getClassInformation(className).getAllInterfaces(cache).toArray(new String[0]);
        Set<String> classAnnotations = cache.getClassInformation(className).classAnnotationNames;
        Set<String> methodAnnotations = cache.getClassInformation(className).methodAnnotationNames;

        Set<PackageValidationResult> matchedPackageResults = this.match(classloader, cache, className, classAnnotations,
                methodAnnotations, superNames, interfaceNames);
        if (matchedPackageResults.isEmpty()) {
            return null;
        }

        ClassNode composite = WeaveUtils.convertToClassNode(targetBytes);
        PackageWeaveResult finalResult = null;

        List<PackageValidationResult> sortedMatchedPackages = new ArrayList<>(matchedPackageResults);
        sortedMatchedPackages.sort(PackageValidationResult.CONFIG_COMPARATOR);
        for (PackageValidationResult weavePackageResult : sortedMatchedPackages) {
            PackageWeaveResult result = weavePackageResult.weave(className, superNames, interfaceNames, composite,
                                                                 cache, skipMethods);
            if (null != weaveListener) {
                weaveListener.classWeaved(result, classloader, cache);
            }
            if (result.weavedClass()) {
                composite = result.getComposite();
                finalResult = result;
            }
        }
        return null == finalResult ? null : finalResult.getCompositeBytes(cache);
    }

    /**
     * Quickly checks to see if the class/super class/interfaces or any of the methods in the Class represented by the
     * "classByte" parameter match the Set of known classes/method signatures that constitute all of our weave classes.
     * If a potential match is found it means we should continue with the normal validation process. If no match was
     * found, then it's certain that this class would never match and we should end transformation of this class via the
     * weaver here.
     *
     * @param classBytes the byte[] representing the class to check
     * @param requiredClasses the set of known classes/superclasses/interfaces to compare against
     * @param methodSignatures the set of all method signatures to compare against
     * @param cache class cache to lookup interface annotations
     * @return true if this contains a possible match, false otherwise
     */
    private boolean containsPossibleClassOrMethodMatch(final String className, final byte[] classBytes, final Set<String> requiredClasses,
                                                       final Set<String> methodSignatures, final ClassCache cache) {
        final AtomicBoolean containsPossibleMatch = new AtomicBoolean(false);
        ClassReader originalBytesReader = new ClassReader(classBytes);
        originalBytesReader.accept(new ClassVisitor(WeaveUtils.ASM_API_LEVEL) {

            public String[] interfaces;
            private boolean isInterface = false;

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {

                this.interfaces = interfaces;
                isInterface = (access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE;

                // Figure out if this exact class or the super class is a match
                if (requiredClasses.contains(name) || requiredClasses.contains(superName)) {
                    containsPossibleMatch.set(true);
                    return;
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                String annotationClass = Type.getType(desc).getClassName();

                if (!isInterface && requiredAnnotationClasses.contains(annotationClass)) {
                    // Adding class name here allows us to catch interfaces
                    containsPossibleMatch.set(true);
                    requiredClasses.add(className);

                    // Found a potential match. Exit early.
                    return null;
                }
                return super.visitAnnotation(desc, visible);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                if (containsPossibleMatch.get()) {
                    // This means we found a class/superclass/interface match so we should exit early here
                    return null;
                }

                // Default constructor and static initializer will be matched by visit() method above
                if (!(name.equals(WeaveUtils.INIT_NAME) && desc.equals(WeaveUtils.INIT_DESC)) &&
                        !(name.equals(WeaveUtils.CLASS_INIT_NAME) && desc.equals(WeaveUtils.INIT_DESC)) &&
                        methodSignatures.contains(name + desc)) {
                    // If any method matches this is a potential match so we should return "true"
                    containsPossibleMatch.set(true);

                    // Skip out of this method visitor early since we've found a match
                    return null;
                }

                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (!requiredMethodAnnotationClasses.isEmpty()) {
                    return new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            String annotationClass = Type.getType(desc).getClassName();
                            if (requiredMethodAnnotationClasses.contains(annotationClass)) {
                                containsPossibleMatch.set(true);

                                // Found a potential match. Exit early.
                                return null;
                            }
                            return super.visitAnnotation(desc, visible);
                        }
                    };
                }
                return mv;
            }

            @Override
            public void visitEnd() {
                // If nothing has matched, check interface annotations
                if (!isInterface && !requiredAnnotationClasses.isEmpty()) {

                    try {
                        for (String interfaceName : interfaces) {
                            ClassInformation interfaceInfo = cache.getClassInformation(interfaceName);
                            if (interfaceInfo == null) {
                                continue;
                            }

                            for (String interfaceAnnotationName : interfaceInfo.classAnnotationNames) {
                                if (requiredAnnotationClasses.contains(WeaveUtils.getClassBinaryName(interfaceAnnotationName))) {
                                    containsPossibleMatch.set(true);
                                    return;
                                }
                            }
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        }, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);

        return containsPossibleMatch.get();
    }

    /**
     * Returns the successful validation result for the weave package against the classloader, or <code>null</code> if
     * there is no successful result.
     */
    private PackageValidationResult getSuccessfulValidation(String className, String superName, String[] interfaceNames, ClassLoader classloader,
            ClassCache cache, WeavePackage weavePackage) throws IOException {
        // If we already have a completed result on this thread we need to use it rather than attempting to re-validate for the same package
        PackageValidationResult localValidationResult = currentValidationResult.get();
        if (localValidationResult != null) {
            return localValidationResult;
        }

        if (validateAgainstClassLoader(className, superName, interfaceNames, classloader, cache, weavePackage)) {
            final ClassLoader classloaderToUse = weavePackage.weavesBootstrap() ? BootstrapLoader.PLACEHOLDER : classloader;
            ConcurrentMap<WeavePackage, PackageValidationResult> valid = validPackages.getIfPresent(classloaderToUse);
            return valid == null ? null : valid.get(weavePackage);
        }
        return null;
    }

    /**
     * Determine if the weavePackage is valid against classloader (using cache to check for resources).
     */
    private boolean validateAgainstClassLoader(String className, String superName, String[] interfaceNames, ClassLoader classloader, ClassCache cache,
            WeavePackage weavePackage) throws IOException {
        if (classloader != BootstrapLoader.PLACEHOLDER && weavePackage.weavesBootstrap()) {
            classloader = BootstrapLoader.PLACEHOLDER;
            cache = new ClassCache(BootstrapLoader.get());
        }

        try {
            // this is the first time we've validated this package against this classloader.
            if (!hasValidated(classloader, weavePackage)) {
                PackageValidationResult verificationResult = weavePackage.validate(cache);
                currentValidationResult.set(verificationResult);

                if (null != packageListener) {
                    packageListener.validated(verificationResult, classloader);
                }

                if ((classloader == BootstrapLoader.PLACEHOLDER && !this.canWeaveBootstrapClassLoader())
                        || (!verificationResult.succeeded())) {
                    ConcurrentMap<WeavePackage, PackageValidationResult> result = invalidPackages.asMap().putIfAbsent(
                            classloader, new ConcurrentHashMap<WeavePackage, PackageValidationResult>());
                    if (result == null) {
                        result = invalidPackages.asMap().get(classloader);
                    }
                    result.put(weavePackage, verificationResult);
                    return false;
                } else {
                    // We need to add this to the valid packages list before appending new classes to prevent a circular class load
                    ConcurrentMap<WeavePackage, PackageValidationResult> result = validPackages.asMap().putIfAbsent(
                            classloader, new ConcurrentHashMap<WeavePackage, PackageValidationResult>());
                    if (result == null) {
                        result = validPackages.asMap().get(classloader);
                    }

                    try {
                        if (BootstrapLoader.PLACEHOLDER == classloader) {
                            NewClassAppender.appendClassesToBootstrapClassLoader(instrumentation,
                                    verificationResult.computeUtilityClassBytes(cache));
                        } else {
                            NewClassAppender.appendClasses(className, superName, interfaceNames, classloader,
                                    verificationResult.computeUtilityClassBytes(cache));
                        }
                        result.put(weavePackage, verificationResult);
                    } catch (Throwable t) {
                        // If the new class appending above throws an exception we need to remove the validated package (since it's no longer valid)
                        result.remove(weavePackage);
                    }
                }
            }

            ConcurrentMap<WeavePackage, PackageValidationResult> result = validPackages.getIfPresent(classloader);
            return result != null && result.containsKey(weavePackage);
        } finally {
            currentValidationResult.remove();
        }
    }

    /**
     * Determine whether the specified package has been validated by the specified class loader.
     */
    private boolean hasValidated(ClassLoader classloader, WeavePackage weavePackage) {
        ConcurrentMap<WeavePackage, PackageValidationResult> invalidResult = invalidPackages.getIfPresent(classloader);
        ConcurrentMap<WeavePackage, PackageValidationResult> validResult = validPackages.getIfPresent(classloader);
        return (invalidResult != null && invalidResult.containsKey(weavePackage)) || (validResult != null
                && validResult.containsKey(weavePackage));
    }

    /**
     * This method reduces the list of weave packages down to only those that could potentially match based on the
     * classes that currently exist on the classpath and the classes that the weave package requires.
     *
     * After determining which packages could match, we cache this value for use with all future calls to match() for a
     * big performance increase. If a new package is registered we invalidate the cache and the next call to match()
     * will take the hit to rebuild the cache to potentially include the newly added package.
     *
     * @param classloader the classloader to check for required classes
     * @param cache the class cache (backed by the classloader) to check if the resource exists
     * @return an optimized map of weave packages for faster matching
     */
    private Map<String, WeavePackage> getOptimizedWeavePackages(ClassLoader classloader, ClassCache cache) {
        ConcurrentMap<String, WeavePackage> classloaderWeavePackages = optimizedWeavePackages.getIfPresent(classloader);
        if (classloaderWeavePackages == null) {
            classloaderWeavePackages = new ConcurrentHashMap<>();

            // Loop over each weave package and verify that all required classes can be found by the classloader
            Collection<WeavePackage> unmatchedWeavePackages = new ArrayList<>();
            for (Map.Entry<String, WeavePackage> entry : this.weavePackages.entrySet()) {
                boolean packageExists = true;
                boolean hasAtLeastOneMatch = false;
                for (String weaveClass : entry.getValue().getRequiredClasses()) {
                    if (!cache.hasClassResource(weaveClass)) {
                        // The class can't be found via this classloader so we'll set the flag
                        // to skip this package but we want to continue this loop to see if we
                        // can find any matching class resources for debugging purposes
                        packageExists = false;
                    } else {
                        // If at least one required class is found lets attempt to validate
                        // the package so we can send the correct debug logging to the user
                        hasAtLeastOneMatch = true;

                        // Optimization to break out of this loop early
                        if (!packageExists) {
                            break;
                        }
                    }
                }

                if (packageExists) {
                    classloaderWeavePackages.put(entry.getKey(), entry.getValue());
                } else if (hasAtLeastOneMatch) {
                    unmatchedWeavePackages.add(entry.getValue());
                }
            }

            ConcurrentMap<String, WeavePackage> optimizedMap = optimizedWeavePackages.asMap().putIfAbsent(classloader,
                    classloaderWeavePackages);
            if (optimizedMap == null) {
                // Run verifications against unmatched packages since this is
                // the thread that added the packages to the optimized map
                for (WeavePackage unmatchedWeavePackage : unmatchedWeavePackages) {
                    try {
                        PackageValidationResult verificationResult = unmatchedWeavePackage.validate(cache);
                        if (null != packageListener) {
                            packageListener.validated(verificationResult, classloader);
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }
        return classloaderWeavePackages;
    }
}
