/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.instrumentation.annotationmatchers.AnnotationMatcher;
import com.newrelic.weave.weavepackage.WeavePackageConfig;

import java.util.Collection;
import java.util.Set;

public interface ClassTransformerConfig extends Config {

    boolean isCustomTracingEnabled();

    Set<String> getExcludes();

    Set<String> getIncludes();

    Set<String> getClassloaderExclusions();

    Set<String> getClassloaderDelegationExcludes();

    Set<String> getClassloaderDelegationIncludes();

    boolean isDefaultInstrumentationEnabled();

    boolean isBuiltinExtensionEnabled();

    /**
     * @return true if ASM should compute stack map frames.
     */
    boolean computeFrames();

    /**
     * Stop class transformation after this many nanoseconds.
     *
     * @return the number of nanoseconds to delay before stopping class transformation, or -1 if never stop
     */
    long getShutdownDelayInNanos();

    boolean isEnabled();

    Collection<String> getJdbcStatements();

    AnnotationMatcher getIgnoreTransactionAnnotationMatcher();

    AnnotationMatcher getIgnoreApdexAnnotationMatcher();

    AnnotationMatcher getTraceAnnotationMatcher();

    /**
     * Returns the maximum number of classloaders to run through the "optimized" weave package path. This allows us to
     * get the benefits of optimizing for apps with a small number of classloaders while ensuring that we don't end up
     * trying to validate too many dynamic classloaders.
     *
     * @return the maximum number of classloaders to pre-validate weave packages for
     */
    int getMaxPreValidatedClassLoaders();

    /**
     * Returns true when we should take the "optimized" path for reducing the number of weave packages that we should
     * check during each classload. For well-behaved (non-dynamic) classloaders the optimized path generally works well,
     * however in the dynamic case this setting can end up having extremely detrimental affects to the startup time
     * of the agent. This should be disabled by default.
     *
     * @return true when prevalidation of weave packages should happen for each classloader
     */
    boolean preValidateWeavePackages();

    /**
     * Returns true when we should attempt to "pre-match" incoming class loads by comparing the method signatures of
     * the class against all of the possible method signatures in our weave packages. This allows us to quickly filter
     * out classes that we know will never match (because there are no matching method descriptors) in order to reduce
     * the likelihood of deadlocks as well as improving startup & runtime.
     *
     * By default this is enabled but it can be disabled if unforeseen problems arise.
     *
     * @return true when we should attempt to pre-match classes based on method signatures, false otherwise.
     */
    boolean preMatchWeaveMethods();

    /**
     * True means the agent should instrument {@link ClassLoader#checkPackageAccess} to bypass the call to
     * {@link SecurityManager#checkPackageAccess} for weaved classes.
     */
    boolean isGrantPackageAccess();

    /**
     * Returns the auto async-link rate limit in millis.
     */
    long getAutoAsyncLinkRateLimit();

    Config getInstrumentationConfig(String implementationTitle);

    /**
     * Indicates whether the agent should load the specified weave package.
     *
     * @param weavePackageConfig Weave package configuration (from module's MANIFEST)
     * @return whether the agent should load the specified weave package
     */
    boolean isWeavePackageEnabled(WeavePackageConfig weavePackageConfig);

    boolean isDefaultMethodTracingEnabled();

    boolean isEnhancedSpringTransactionNaming();

    /**
     * When true, Spring Controller transactions will be named using the controller class name and method name
     * (e.g., "CustomerController.edit") instead of using request mappings (e.g., "/api/v1/customer (POST)").
     * This helps prevent transaction name cardinality issues with complex URI patterns.
     *
     * @return true if Spring Controller transactions should use class name + method name format
     */
    boolean useControllerClassForSpringTransactionNaming();
}
