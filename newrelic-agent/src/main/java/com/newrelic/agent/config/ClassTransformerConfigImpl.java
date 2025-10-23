/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.annotationmatchers.AnnotationMatcher;
import com.newrelic.agent.instrumentation.annotationmatchers.AnnotationNames;
import com.newrelic.agent.instrumentation.annotationmatchers.ClassNameAnnotationMatcher;
import com.newrelic.agent.instrumentation.annotationmatchers.NoMatchAnnotationMatcher;
import com.newrelic.agent.instrumentation.annotationmatchers.OrAnnotationMatcher;
import com.newrelic.weave.weavepackage.WeavePackageConfig;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

final class ClassTransformerConfigImpl extends BaseConfig implements ClassTransformerConfig {

    public static final String ENABLED = "enabled";
    public static final String EXCLUDES = "excludes";
    public static final String INCLUDES = "includes";
    @Deprecated
    public static final String CLASSLOADER_BLACKLIST = "classloader_blacklist";
    public static final String CLASSLOADER_DELEGATION_EXCLUDES = "classloader_delegation_excludes";
    public static final String CLASSLOADER_DELEGATION_INCLUDES = "classloader_delegation_includes";
    public static final String CLASSLOADER_EXCLUDES = "classloader_excludes";
    public static final String MAX_PREVALIDATED_CLASSLOADERS = "max_prevalidated_classloaders";
    public static final String PREVALIDATE_WEAVE_PACKAGES = "prevalidate_weave_packages";
    public static final String PREMATCH_WEAVE_METHODS = "prematch_weave_methods";
    public static final String DEFAULT_INSTRUMENTATION = "instrumentation_default";
    public static final String BUILTIN_EXTENSIONS = "builtin_extensions";
    public static final String COMPUTE_FRAMES = "compute_frames";
    public static final String SHUTDOWN_DELAY = "shutdown_delay";
    public static final String GRANT_PACKAGE_ACCESS = "grant_package_access";
    public static final String ENHANCED_SPRING_TRANSACTION_NAMING = "enhanced_spring_transaction_naming";
    public static final String USE_CONTROLLER_CLASS_AND_METHOD_FOR_SPRING_TRANSACTION_NAMING = "use_controller_class_and_method_for_spring_transaction_naming";
    public static final boolean DEFAULT_COMPUTE_FRAMES = true;
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_DISABLED = false;
    public static final int DEFAULT_SHUTDOWN_DELAY = -1;
    public static final boolean DEFAULT_GRANT_PACKAGE_ACCESS = false;
    public static final int DEFAULT_MAX_PREVALIDATED_CLASSLOADERS = 10;
    public static final boolean DEFAULT_PREVALIDATE_WEAVE_PACKAGES = true;
    public static final boolean DEFAULT_PREMATCH_WEAVE_METHODS = true;
    public static final boolean DEFAULT_ENHANCED_SPRING_TRANSACTION_NAMING = false;
    public static final boolean DEFAULT_USE_CONTROLLER_CLASS_AND_METHOD_FOR_SPRING_TRANSACTION_NAMING = false;

    private static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.class_transformer.";

    static final String NEW_RELIC_TRACE_TYPE_DESC = "Lcom/newrelic/api/agent/Trace;";
    static final String OTEL_WITH_SPAN_TYPE_DESC = "Lio/opentelemetry/instrumentation/annotations/WithSpan;";
    static final String DEPRECATED_NEW_RELIC_TRACE_TYPE_DESC = "Lcom/newrelic/agent/Trace;";

    // as of JAVA-4824 the yml config file is not required, but still need to match old behavior
    private static final Set<String> DEFAULT_DISABLED_WEAVE_PACKAGES = new HashSet<>();
    private static final Set<String> DEFAULT_CLASSLOADER_EXCLUDES = new HashSet<>();

    // Security agent specific excludes needed to allow functioning with java.io.InputStream and OutputStream instrumentation.
    private static final Set<String> SECURITY_AGENT_CLASS_EXCLUDES = new HashSet<>(Arrays.asList("java/util/zip/InflaterInputStream", "java/util/zip/ZipFile$ZipFileInputStream",
            "java/util/zip/ZipFile$ZipFileInflaterInputStream", "com/newrelic/api/agent/security/.*", "com/newrelic/agent/security/.*"));

    static {
        DEFAULT_DISABLED_WEAVE_PACKAGES.add("com.newrelic.instrumentation.servlet-user");
        DEFAULT_DISABLED_WEAVE_PACKAGES.add("com.newrelic.instrumentation.spring-aop-2");
        DEFAULT_DISABLED_WEAVE_PACKAGES.add("com.newrelic.instrumentation.jdbc-resultset");

        DEFAULT_CLASSLOADER_EXCLUDES.add("groovy.lang.GroovyClassLoader$InnerLoader");
        DEFAULT_CLASSLOADER_EXCLUDES.add("org.codehaus.groovy.runtime.callsite.CallSiteClassLoader");
        DEFAULT_CLASSLOADER_EXCLUDES.add("com.collaxa.cube.engine.deployment.BPELClassLoader");
        DEFAULT_CLASSLOADER_EXCLUDES.add("org.springframework.data.convert.ClassGeneratingEntityInstantiator$ObjectInstantiatorClassGenerator");
        DEFAULT_CLASSLOADER_EXCLUDES.add("org.mvel2.optimizers.impl.asm.ASMAccessorOptimizer$ContextClassLoader");
        DEFAULT_CLASSLOADER_EXCLUDES.add("gw.internal.gosu.compiler.SingleServingGosuClassLoader");
    }

    private final boolean isEnabled;
    private final boolean custom_tracing;
    private final Set<String> excludes;
    private final Set<String> includes;
    private final Set<String> classloaderExclusions;
    private final Set<String> classloaderDelegationExcludes;
    private final Set<String> classloaderDelegationIncludes;
    private final boolean computeFrames;
    private final boolean isDefaultInstrumentationEnabled;
    private final long shutdownDelayInNanos;
    private final boolean grantPackageAccess;
    private final int maxPreValidatedClassLoaders;
    private final boolean preValidateWeavePackages;
    private final boolean preMatchWeaveMethods;
    private final boolean isEnhancedSpringTransactionNaming;
    private final boolean useControllerClassForSpringTransactionNaming;

    private final AnnotationMatcher ignoreTransactionAnnotationMatcher;
    private final AnnotationMatcher ignoreApdexAnnotationMatcher;
    private final AnnotationMatcher traceAnnotationMatcher;
    private final boolean defaultMethodTracingEnabled;
    private final boolean isBuiltinExtensionEnabled;
    private final boolean litemode;
    private final long autoAsyncLinkRateLimit;

    public ClassTransformerConfigImpl(Map<String, Object> props, boolean customTracingEnabled, boolean litemode, boolean addSecurityExcludes) {
        super(props, SYSTEM_PROPERTY_ROOT);
        this.custom_tracing = customTracingEnabled;
        this.litemode = litemode;
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        isDefaultInstrumentationEnabled = getDefaultInstrumentationEnabled();
        isBuiltinExtensionEnabled = getBuiltinExensionEnabled();
        excludes = initializeClassExcludes(addSecurityExcludes);
        includes = Collections.unmodifiableSet(new HashSet<>(getUniqueStrings(INCLUDES)));
        classloaderExclusions = initializeClassloaderExcludes();
        classloaderDelegationExcludes = initializeClassloaderDelegationExcludes();
        classloaderDelegationIncludes = initializeClassloaderDelegationIncludes();
        computeFrames = getProperty(COMPUTE_FRAMES, DEFAULT_COMPUTE_FRAMES);
        shutdownDelayInNanos = initShutdownDelay();
        grantPackageAccess = getProperty(GRANT_PACKAGE_ACCESS, DEFAULT_GRANT_PACKAGE_ACCESS);
        maxPreValidatedClassLoaders = getProperty(MAX_PREVALIDATED_CLASSLOADERS, DEFAULT_MAX_PREVALIDATED_CLASSLOADERS);
        preValidateWeavePackages = getProperty(PREVALIDATE_WEAVE_PACKAGES, DEFAULT_PREVALIDATE_WEAVE_PACKAGES);
        preMatchWeaveMethods = getProperty(PREMATCH_WEAVE_METHODS, DEFAULT_PREMATCH_WEAVE_METHODS);
        defaultMethodTracingEnabled = getProperty("default_method_tracing_enabled", true);
        autoAsyncLinkRateLimit = getProperty("auto_async_link_rate_limit", TimeUnit.SECONDS.toMillis(1));
        isEnhancedSpringTransactionNaming = getProperty(ENHANCED_SPRING_TRANSACTION_NAMING, DEFAULT_ENHANCED_SPRING_TRANSACTION_NAMING);
        useControllerClassForSpringTransactionNaming = getProperty(USE_CONTROLLER_CLASS_AND_METHOD_FOR_SPRING_TRANSACTION_NAMING, DEFAULT_USE_CONTROLLER_CLASS_AND_METHOD_FOR_SPRING_TRANSACTION_NAMING);

        this.traceAnnotationMatcher = customTracingEnabled ? initializeTraceAnnotationMatcher(props) : new NoMatchAnnotationMatcher();
        this.ignoreTransactionAnnotationMatcher = new ClassNameAnnotationMatcher(AnnotationNames.NEW_RELIC_IGNORE_TRANSACTION, false);
        this.ignoreApdexAnnotationMatcher = new ClassNameAnnotationMatcher(AnnotationNames.NEW_RELIC_IGNORE_APDEX, false);
    }

    public ClassTransformerConfigImpl(Map<String, Object> props, boolean customTracingEnabled) {
        this(props, customTracingEnabled, false, false);
    }

    private boolean getBuiltinExensionEnabled() {
        Boolean builtinExtensionEnabled = getInstrumentationConfig(BUILTIN_EXTENSIONS).getProperty(ENABLED);
        if (!isDefaultInstrumentationEnabled && (builtinExtensionEnabled == null || !builtinExtensionEnabled)) {
            return DEFAULT_DISABLED;
        } else {
            return DEFAULT_ENABLED;
        }
    }

    private boolean getDefaultInstrumentationEnabled() {
        Boolean defaultInstrumentationEnabled = getInstrumentationConfig(DEFAULT_INSTRUMENTATION).getProperty(ENABLED);
        if (litemode) {
            return false;
        }
        if (defaultInstrumentationEnabled == null || defaultInstrumentationEnabled) {
            return DEFAULT_ENABLED;
        } else {
            Agent.LOG.info("Instrumentation is disabled by default");
            return DEFAULT_DISABLED;
        }
    }

    private Set<String> initializeClassExcludes(boolean addSecurityExcludes) {
        HashSet<String> tempExcludes = new HashSet<>(getUniqueStrings(EXCLUDES));
        if (addSecurityExcludes) {
            tempExcludes.addAll(SECURITY_AGENT_CLASS_EXCLUDES);
        }

        return Collections.unmodifiableSet(new HashSet<>(tempExcludes));
    }

    private Set<String> initializeClassloaderExcludes() {
        Set<String> classloadersToExclude = new HashSet<>(getUniqueStrings(CLASSLOADER_EXCLUDES));

        // We released an undocumented property called classloader_blacklist, and renamed it to classloader_excludes
        // before making it public in 3.30.0.
        addDeprecatedProperty(
                new String[] { AgentConfigImpl.CLASS_TRANSFORMER, CLASSLOADER_BLACKLIST },
                new String[] { AgentConfigImpl.CLASS_TRANSFORMER, CLASSLOADER_EXCLUDES }
        );
        if (classloadersToExclude.isEmpty()) {
            classloadersToExclude.addAll(getUniqueStrings(CLASSLOADER_EXCLUDES));
        }

        classloadersToExclude.addAll(DEFAULT_CLASSLOADER_EXCLUDES);
        return Collections.unmodifiableSet(classloadersToExclude);
    }

    private Set<String> initializeClassloaderDelegationExcludes() {
        Set<String> classloadersToExclude = new HashSet<>(getUniqueStrings(CLASSLOADER_DELEGATION_EXCLUDES));
        return Collections.unmodifiableSet(classloadersToExclude);
    }

    private Set<String> initializeClassloaderDelegationIncludes() {
        Set<String> classloadersToInclude = new HashSet<>(getUniqueStrings(CLASSLOADER_DELEGATION_INCLUDES));
        return Collections.unmodifiableSet(classloadersToInclude);
    }

    private AnnotationMatcher initializeTraceAnnotationMatcher(Map<?, ?> props) {
        List<AnnotationMatcher> matchers = new ArrayList<>();
        matchers.add(new ClassNameAnnotationMatcher(Type.getType(DEPRECATED_NEW_RELIC_TRACE_TYPE_DESC).getDescriptor()));
        matchers.add(new ClassNameAnnotationMatcher(Type.getType(NEW_RELIC_TRACE_TYPE_DESC).getDescriptor()));
        matchers.add(new ClassNameAnnotationMatcher(Type.getType(OTEL_WITH_SPAN_TYPE_DESC).getDescriptor()));

        final Collection<String> traceAnnotationClassNames = getUniqueStrings("trace_annotation_class_name");
        if (traceAnnotationClassNames.isEmpty()) {
            matchers.add(new ClassNameAnnotationMatcher("NewRelicTrace", false));
        } else {
            final Set<String> internalizedNames = new HashSet<>();
            for (String name : traceAnnotationClassNames) {
                Agent.LOG.fine("Adding " + name + " as a Trace annotation");
                internalizedNames.add(internalizeName(name));
            }
            matchers.add(internalizedNames::contains);
        }
        return OrAnnotationMatcher.getOrMatcher(matchers.toArray(new AnnotationMatcher[0]));
    }

    static String internalizeName(String name) {
        return 'L' + name.trim().replace('.', '/') + ';';
    }

    private long initShutdownDelay() {
        int shutdownDelayInSeconds = getIntProperty(SHUTDOWN_DELAY, DEFAULT_SHUTDOWN_DELAY);
        if (shutdownDelayInSeconds > 0) {
            return TimeUnit.NANOSECONDS.convert(shutdownDelayInSeconds, TimeUnit.SECONDS);
        }
        return -1L;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean isCustomTracingEnabled() {
        return custom_tracing;
    }

    @Override
    public Set<String> getIncludes() {
        return includes;
    }

    @Override
    public Set<String> getClassloaderExclusions() {
        return classloaderExclusions;
    }

    @Override
    public Set<String> getClassloaderDelegationExcludes() {
        return classloaderDelegationExcludes;
    }

    @Override
    public Set<String> getClassloaderDelegationIncludes() {
        return classloaderDelegationIncludes;
    }

    @Override
    public boolean isDefaultInstrumentationEnabled() {
        return isDefaultInstrumentationEnabled;
    }

    @Override
    public boolean isBuiltinExtensionEnabled() {
        return isBuiltinExtensionEnabled;
    }

    @Override
    public Set<String> getExcludes() {
        return excludes;
    }

    @Override
    public boolean computeFrames() {
        return computeFrames;
    }

    @Override
    public boolean isGrantPackageAccess() {
        return grantPackageAccess;
    }

    @Override
    public long getShutdownDelayInNanos() {
        return shutdownDelayInNanos;
    }

    @Override
    public final AnnotationMatcher getIgnoreTransactionAnnotationMatcher() {
        return ignoreTransactionAnnotationMatcher;
    }

    @Override
    public final AnnotationMatcher getIgnoreApdexAnnotationMatcher() {
        return ignoreApdexAnnotationMatcher;
    }

    @Override
    public AnnotationMatcher getTraceAnnotationMatcher() {
        return traceAnnotationMatcher;
    }

    @Override
    public int getMaxPreValidatedClassLoaders() {
        return maxPreValidatedClassLoaders;
    }

    @Override
    public boolean preValidateWeavePackages() {
        return preValidateWeavePackages;
    }

    @Override
    public boolean preMatchWeaveMethods() {
        return preMatchWeaveMethods;
    }

    @Override
    public long getAutoAsyncLinkRateLimit() {
        return autoAsyncLinkRateLimit;
    }

    @Override
    public boolean isEnhancedSpringTransactionNaming() {
        return isEnhancedSpringTransactionNaming;
    }

    @Override
    public boolean useControllerClassForSpringTransactionNaming() {
        return useControllerClassForSpringTransactionNaming;
    }

    public static final String JDBC_STATEMENTS_PROPERTY = "jdbc_statements";

    @Override
    public Collection<String> getJdbcStatements() {
        String jdbcStatementsProp = getProperty(JDBC_STATEMENTS_PROPERTY);
        if (jdbcStatementsProp == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(jdbcStatementsProp.split(",[\\s]*"));
    }

    static ClassTransformerConfig createClassTransformerConfig(Map<String, Object> settings, boolean custom_tracing, boolean litemode, boolean addSecurityExcludes) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new ClassTransformerConfigImpl(settings, custom_tracing, litemode, addSecurityExcludes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Config getInstrumentationConfig(String implementationTitle) {
        Map<String, Object> config = Collections.emptyMap();
        if (implementationTitle != null) {
            Object pointCutConfig = getProperty(implementationTitle);
            if (pointCutConfig instanceof Map) {
                config = (Map<String, Object>) pointCutConfig; // SuppressWarnings applied here
            }
        }
        return new BaseConfig(config, SYSTEM_PROPERTY_ROOT + implementationTitle + ".");
    }

    @Override
    public boolean isWeavePackageEnabled(WeavePackageConfig weavePackageConfig) {
        // user can override with traditional instrumentation module name
        String moduleName = weavePackageConfig.getName();
        Config instrumentationConfig = getInstrumentationConfig(moduleName);
        Boolean moduleNameEnabled = instrumentationConfig.getProperty("enabled");

        if (moduleNameEnabled == null) {
            if (DEFAULT_DISABLED_WEAVE_PACKAGES.contains(moduleName)) {
                moduleNameEnabled = false;
            }
        }

        // ..or, an alias for backwards compatibility with legacy pointcut configurations, if available
        String aliasName = weavePackageConfig.getAlias();
        Boolean aliasEnabled = null;
        if (aliasName != null) {
            Config aliasConfig = getInstrumentationConfig(aliasName);
            if (aliasConfig.getProperty("enabled") != null) {
                Agent.LOG.log(Level.FINE, "An alias was used configuring instrumentation: {1} using alias {0}", aliasName, moduleName);
                aliasEnabled = aliasConfig.getProperty("enabled");
            }
        }

        Agent.LOG.log(Level.FINEST, " ### Considering instrumentation: {0}({1}) enabled?{2}({3})", moduleName, aliasName, moduleNameEnabled, aliasEnabled);

        if (moduleNameEnabled == null && aliasEnabled == null && !isDefaultInstrumentationEnabled) {
            Agent.LOG.log(Level.FINEST, " Instrumentation is disabled by default. Skipping: {0} because it is not explicitly enabled.", moduleName);
            return false; // no configuration and instrumentation_disabled is true
        } else if (moduleNameEnabled == null && aliasEnabled == null && isDefaultInstrumentationEnabled) {
            return weavePackageConfig.isEnabled(); // no configuration, return default from weave package
        } else if (moduleNameEnabled == null) {
            return aliasEnabled; // only alias was configured
        } else if (aliasEnabled == null) {
            return moduleNameEnabled; // only module name was configured
        } else {
            return aliasEnabled && moduleNameEnabled; // both were configured
        }
    }

    @Override
    public boolean isDefaultMethodTracingEnabled() {
        return defaultMethodTracingEnabled;
    }
}
