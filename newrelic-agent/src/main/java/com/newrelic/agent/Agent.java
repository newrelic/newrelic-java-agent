/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.JarResource;
import com.newrelic.agent.config.JavaVersionUtils;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.core.CoreServiceImpl;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.ServiceManagerImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.util.UnwindableInstrumentation;
import com.newrelic.agent.util.UnwindableInstrumentationImpl;
import com.newrelic.agent.util.asm.ClassStructure;
import com.newrelic.api.agent.security.NewRelicSecurity;
import com.newrelic.bootstrap.BootstrapAgent;
import com.newrelic.bootstrap.BootstrapLoader;
import com.newrelic.bootstrap.EmbeddedJarFilesImpl;
import com.newrelic.weave.utils.Streams;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static com.newrelic.agent.config.SecurityAgentConfig.isSecurityEnabled;
import static com.newrelic.agent.config.SecurityAgentConfig.shouldInitializeSecurityAgent;
import static com.newrelic.agent.config.SecurityAgentConfig.addSecurityAgentConfigSupportabilityMetrics;

/**
 * New Relic Agent class. The premain you see here is but a fleeting shadow of the true premain. The real premain,
 * called directly by the JVM in response to the -javaagent flag, can be found in BootstrapAgent.java.
 */
public final class Agent {

    /**
     * Access to logger. Implementation note: logging is directed to the console until the AgentService is initialized
     * by the ServiceManager. This occurs during ServiceManager.start(). Prior to initialization, the log level is INFO.
     */
    public static final IAgentLogger LOG = AgentLogManager.getLogger();

    private static final String NEWRELIC_BOOTSTRAP = "newrelic-bootstrap";
    private static final String AGENT_ENABLED_PROPERTY = "newrelic.config.agent_enabled";

    private static final String VERSION = Agent.initVersion();

    private static long agentPremainTime;

    public static String getVersion() {
        return VERSION;
    }

    /**
     * Get the agent version from Agent.properties.
     */
    private static String initVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(Agent.class.getName());
            return bundle.getString("version");
        } catch (Throwable t) {
        }
        return "0.0";
    }

    public static boolean isDebugEnabled() {
        return DebugFlag.DEBUG;
    }

    private static volatile boolean canFastPath = true;

    /**
     * Return true if this Agent can use "fast path" optimizations.
     *
     * @return true if this Agent can use "fast path" optimizations. Fast path optimizations cannot be enabled if
     * certain legacy instrumentation is in use (and possibly for other reasons as well).
     */
    public static boolean canFastPath() {
        return canFastPath;
    }

    /**
     * Disable "fast path" optimizations for the lifetime of this Agent.
     */
    public static void disableFastPath() {
        // The pre-check on the volatile variable is *believed* to be important for performance, but this has
        // yet to be confirmed by experiment using JMH. This particular variable implements a kind of latch:
        // that changes state at most once during the lifetime of the JVM. Therefore, it only needs to be modified
        // if it's in the default state, and then never again. In this special case, the hypothesis is that we
        // retain the virtues of read-sharing for the cache line containing the volatile if we always check
        // before writing to it.
        if (canFastPath) {
            canFastPath = false;
        }
    }

    /**
     * Called by the "real" premain() in the BootstrapAgent.
     */
    @SuppressWarnings("unused")
    public static void continuePremain(String agentArgs, Instrumentation inst, long startTime) {
        inst = maybeWrapInstrumentation(inst);
        final LifecycleObserver lifecycleObserver = LifecycleObserver.createLifecycleObserver(agentArgs);
        if (!lifecycleObserver.isAgentSafe()) {
            return;
        }
        // This *MUST* be done first thing in the premain
        addMixinInterfacesToBootstrap(inst);

        // Although the logger is statically initialized at the top of this class, it will only write to the standard
        // output until it is configured. This occurs within ServiceManager.start() via a call back to the doStart()
        // method, above in this class. The "log" statements below go only to the console. During this time, the logger
        // runs at INFO level. We could use println here, but it's easier to keep the LOG calls rather than duplicating
        // their textual output format.

        if (ServiceFactory.getServiceManager() != null) {
            LOG.warning("New Relic Agent is already running! Check if more than one -javaagent switch is used on the command line.");
            lifecycleObserver.agentAlreadyRunning();
            return;
        }
        String enabled = System.getProperty(AGENT_ENABLED_PROPERTY);
        if (enabled != null && !Boolean.parseBoolean(enabled)) {
            LOG.warning("New Relic Agent is disabled by a system property.");
            return;
        }
        String jvmName = System.getProperty("java.vm.name");
        if (jvmName.contains("Oracle JRockit")) {
            String msg = MessageFormat.format("New Relic Agent {0} does not support the Oracle JRockit JVM (\"{1}\").", Agent.getVersion(), jvmName);
            LOG.warning(msg);
        }

        if (!tryToInitializeServiceManager(inst)) {
            return;
        }

        ServiceManager serviceManager = null;
        try {
            serviceManager = ServiceFactory.getServiceManager();

            // The following method will immediately configure the log so that the rest of our initialization sequence
            // is written to the newrelic_agent.log rather than to the console. Configuring the log also applies the
            // log_level setting from the newrelic.yml so debugging levels become available here, if so configured.
            serviceManager.start();
            lifecycleObserver.serviceManagerStarted(serviceManager);

            LOG.info(MessageFormat.format("New Relic Agent v{0} has started", Agent.getVersion()));

            if (System.getProperty("newrelic.bootstrap_classpath") != null) {
                // This is an obsolete system property that caused the entire Agent to be loaded on the bootstrap.
                LOG.info("The \"newrelic.bootstrap_classpath\" property is no longer used. Please remove it from your configuration.");
            }
            LOG.info("Agent class loader: " + AgentBridge.getAgent().getClass().getClassLoader());

            logAnyFilesFoundInEndorsedDirs();

            if (serviceManager.getConfigService().getDefaultAgentConfig().isStartupTimingEnabled()) {
                recordPremainTime(serviceManager.getStatsService(), startTime);
            }

            recordAgentVersion(serviceManager.getStatsService());
        } catch (Throwable t) {
            // There's no way to gracefully pull the agent out due to our bytecode modification and class structure changes (pointcuts).
            // We're likely to throw an exception into the user's app if we try to continue.
            String msg = "Unable to start New Relic Agent. Please remove -javaagent from your startup arguments and contact New Relic support.";
            try {
                LOG.log(Level.SEVERE, t, msg);
            } catch (Throwable t2) {
            }
            System.err.println(msg);

            if (t instanceof NoClassDefFoundError) {
                String version = System.getProperty("java.version");
                if (version.startsWith("9") || version.startsWith("10")) {
                    String message = "We currently do not support Java 9 or 10 in modular mode. If you are running with " +
                            "it and want to use the agent, use command line flag '--add-modules' to add appropriate modules";
                    System.err.println(message);
                } else if (version.startsWith("11") || version.startsWith("12")) {
                    String message = "Applications that previously relied on the command line flag '--add-modules' will no longer work with Java EE " +
                            "dependencies. You must add all Java EE dependencies to your build file manually, and then remove the --add-modules flag for them.";
                    System.err.println(message);
                }
            }
            t.printStackTrace();

            if (inst instanceof UnwindableInstrumentation) {
                final UnwindableInstrumentation instrumentation = (UnwindableInstrumentation) inst;
                if (serviceManager != null) {
                    try {
                        serviceManager.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                LOG.severe("Detaching the New Relic agent");
                instrumentation.unwind();
                LOG.severe("The New Relic agent was detached");
                return;
            } else {
                System.exit(1);
            }
        }
        if (inst instanceof UnwindableInstrumentation) {
            final UnwindableInstrumentation instrumentation = (UnwindableInstrumentation) inst;
            instrumentation.started();
        }
        lifecycleObserver.agentStarted();
        InitialiseNewRelicSecurityIfAllowed(inst);
    }

    private static void InitialiseNewRelicSecurityIfAllowed(Instrumentation inst) {
        // Do not initialise New Relic Security module so that it stays in NoOp mode if force disabled.
        addSecurityAgentConfigSupportabilityMetrics();
        if (shouldInitializeSecurityAgent()) {
            try {
                LOG.log(Level.INFO, "Initializing New Relic Security module");
                ServiceFactory.getServiceManager().getRPMServiceManager().addConnectionListener(new ConnectionListener() {
                    @Override
                    public void connected(IRPMService rpmService, AgentConfig agentConfig) {
                        if (isSecurityEnabled()) {
                            try {
                                URL securityJarURL = EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(BootstrapLoader.NEWRELIC_SECURITY_AGENT).toURI().toURL();
                                LOG.log(Level.INFO, "Connected to New Relic. Starting New Relic Security module");
                                NewRelicSecurity.getAgent().refreshState(securityJarURL, inst);
                                NewRelicSecurity.markAgentAsInitialised();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            LOG.info("New Relic Security is disabled by one of the user provided config `security.enabled` or `high_security`.");
                        }
                    }

                    @Override
                    public void disconnected(IRPMService rpmService) {
                        LOG.log(Level.INFO, "Deactivating New Relic Security module");
                        NewRelicSecurity.getAgent().deactivateSecurity();
                    }
                });
            } catch (Throwable t2) {
                LOG.error("license_key is empty in the config. Not starting New Relic Security Agent.");
            }
        } else {
            LOG.info("New Relic Security is completely disabled by one of the user provided config `security.enabled`, `security.agent.enabled` or `high_security`. Not loading security capabilities.");
        }
    }

    private static Instrumentation maybeWrapInstrumentation(Instrumentation inst) {
        if (System.getProperty(BootstrapAgent.NR_AGENT_ARGS_SYSTEM_PROPERTY) != null) {
            return UnwindableInstrumentationImpl.wrapInstrumentation(inst);
        }
        return inst;
    }

    private static boolean tryToInitializeServiceManager(Instrumentation inst) {
        try {
            CoreService coreService = new CoreServiceImpl(inst);
            ConfigService configService = ConfigServiceFactory.createConfigService(Agent.LOG, System.getProperty("newrelic.checkconfig") != null);
            ServiceManager serviceManager = new ServiceManagerImpl(coreService, configService);
            ServiceFactory.setServiceManager(serviceManager);

            if (isLicenseKeyEmpty(serviceManager.getConfigService().getDefaultAgentConfig().getLicenseKey())) {
                LOG.error("license_key is empty in the config. Not starting New Relic Agent.");
                return false;
            }

            if (!serviceManager.getConfigService().getDefaultAgentConfig().isAgentEnabled()) {
                LOG.warning("agent_enabled is false in the config. Not starting New Relic Agent.");
                return false;
            }

            // Now that we know the agent is enabled, add the ApiClassTransformer
            BootstrapLoader.forceCorrectNewRelicApi(inst);
            BootstrapLoader.forceCorrectNewRelicSecurityApi(inst);

            // init problem classes before class transformer service is active
            InitProblemClasses.loadInitialClasses();
        } catch (ForceDisconnectException e) {
            /* Note: Our use of ForceDisconnectException is a bit misleading here as we haven't even tried to connect
             * to RPM at this point (that happens a few lines down when we call serviceManager.start()). This exception
             * comes from ConfigServiceFactory when it attempts to validate the local yml and finds that both HSM and
             * LASP are enabled. The LASP spec says in this scenario that "Shutdown will follow the behavior of the
             * ForceDisconnectException response from "New Relic." Not specifically that we should throw ForceDisconnectException.
             * Perhaps we should throw a different, more accurately named exception, that simply has the same behavior
             * as ForceDisconnectException as it will be replaced by a 410 response code in Protocol 17.
             */
            LOG.log(Level.SEVERE, e.getMessage());
            return false;
        } catch (Throwable t) {
            // this is the last point where we can stop the agent gracefully if something has gone wrong.
            LOG.log(Level.SEVERE, t, "Unable to start the New Relic Agent. Your application will continue to run but it will not be monitored.");
            return false;
        }
        return true;
    }

    private static void logAnyFilesFoundInEndorsedDirs() {
        String javaEndorsedDirs = System.getProperty("java.endorsed.dirs");
        // The classes in this dir will be loaded directly onto the bootstrap class loader, which may cause
        // NoClassDefFoundError's to occur.
        if (javaEndorsedDirs == null || javaEndorsedDirs.isEmpty()) {
            return;
        }

        try {
            // Split out each directory using the path separator
            String[] endorsedDirs = javaEndorsedDirs.split(String.valueOf(File.pathSeparatorChar));
            for (String endorsedDir : endorsedDirs) {
                File endorsedDirFile = new File(endorsedDir);
                // If the path exists and it's a directory we need to see if there are any files in that directory
                if (!endorsedDirFile.exists() || !endorsedDirFile.isDirectory()) {
                    continue;
                }

                File[] files = endorsedDirFile.listFiles();
                if (files == null || files.length == 0) {
                    continue;
                }

                // The directory has at least one file in it, log a warning about it
                LOG.log(Level.WARNING, "The 'java.endorsed.dirs' system property is set to {0} and that directory is not empty for this jvm. "
                        + "This may cause unexpected behavior.", endorsedDir);
                //Log the contents of the directory at log level FINER
                StringBuilder endorsedDirContent = new StringBuilder();
                for (File file : files) {
                    if (endorsedDirContent.length() > 0) {
                        endorsedDirContent.append(", ");
                    }
                    endorsedDirContent.append(file.getName());
                }
                LOG.log(Level.FINER, "The endorsed directory {0} contains the following items: {1}", endorsedDir,
                        endorsedDirContent.toString());
            }
        } catch (Throwable t) {
            LOG.log(Level.FINE, t, "An unexpected error occurred while checking for java.endorsed.dirs property");
        }
    }

    private static boolean isLicenseKeyEmpty(String licenseKey) {
        return licenseKey == null || licenseKey.isEmpty() || licenseKey.equals("<%= license_key %>");
    }

    public static void main(String[] args) {
        String javaSpecVersion = JavaVersionUtils.getJavaSpecificationVersion();
        String sysExperimentalRuntime = System.getProperty("newrelic.config.experimental_runtime");
        String envExperimentalRuntime = System.getenv("NEW_RELIC_EXPERIMENTAL_RUNTIME");
        boolean useExperimentalRuntime = (Boolean.parseBoolean(sysExperimentalRuntime)
                || ((Boolean.parseBoolean(envExperimentalRuntime))));

        if (useExperimentalRuntime) {
            System.out.println("----------");
            System.out.println(JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(javaSpecVersion));
            System.out.println("Experimental runtime mode is enabled. Usage of the agent in this mode is for experimenting with early access" +
                    " or upcoming Java releases at your own risk.");
            System.out.println("----------");
        }
        if (!JavaVersionUtils.isAgentSupportedJavaSpecVersion(javaSpecVersion) && !useExperimentalRuntime) {
            System.err.println("----------");
            System.err.println(JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(javaSpecVersion));
            System.err.println("----------");
            return;
        }
        new AgentCommandLineParser().parseCommand(args);
    }

    public static long getAgentPremainTimeInMillis() {
        return agentPremainTime;
    }

    private static void recordPremainTime(StatsService statsService, long startTime) {
        agentPremainTime = System.currentTimeMillis() - startTime;
        LOG.log(Level.INFO, "Premain startup complete in {0}ms", agentPremainTime);
        statsService.doStatsWork(StatsWorks.getRecordResponseTimeWork(MetricNames.SUPPORTABILITY_TIMING_PREMAIN, agentPremainTime),
                MetricNames.SUPPORTABILITY_TIMING_PREMAIN);

        Map<String, Object> environmentInfo = ImmutableMap.<String, Object>builder()
                .put("Duration", agentPremainTime)
                .put("Version", getVersion())
                .put("JRE Vendor", System.getProperty("java.vendor"))
                .put("JRE Version", System.getProperty("java.version"))
                .put("JVM Vendor", System.getProperty("java.vm.vendor"))
                .put("JVM Version", System.getProperty("java.vm.version"))
                .put("JVM Runtime Version", System.getProperty("java.runtime.version"))
                .put("OS Name", System.getProperty("os.name"))
                .put("OS Version", System.getProperty("os.version"))
                .put("OS Arch", System.getProperty("os.arch"))
                .put("Processors", Runtime.getRuntime().availableProcessors())
                .put("Free Memory", Runtime.getRuntime().freeMemory())
                .put("Total Memory", Runtime.getRuntime().totalMemory())
                .put("Max Memory", Runtime.getRuntime().maxMemory()).build();
        LOG.log(Level.FINE, "Premain environment info: {0}", environmentInfo.toString());
    }

    /**
     * Records a metric representing the agent version.
     */
    private static void recordAgentVersion(StatsService statsService) {
        statsService.doStatsWork(
                StatsWorks.getIncrementCounterWork(MessageFormat.format(MetricNames.SUPPORTABILITY_JAVA_AGENTVERSION, getVersion()), 1),
                MetricNames.SUPPORTABILITY_JAVA_AGENTVERSION);
    }

    /**
     * Extract all the mixins from the Agent jar and add them to the bootstrap classloader. JAVA-609.
     *
     * @param inst the JVM instrumentation interface
     */
    private static void addMixinInterfacesToBootstrap(Instrumentation inst) {
        if (isDisableMixinsOnBootstrap()) {
            System.out.println("New Relic Agent: mixin interfaces not moved to bootstrap");
            return;
        }
        JarResource agentJarResource = null;
        URL agentJarUrl;
        try {
            agentJarResource = AgentJarHelper.getAgentJarResource();
            agentJarUrl = AgentJarHelper.getAgentJarUrl();
            addMixinInterfacesToBootstrap(agentJarResource, agentJarUrl, inst);
        } finally {
            try {
                agentJarResource.close();
            } catch (Throwable th) {
                logIfNRDebug("closing Agent jar resource", th);
            }
        }
    }

    /**
     * Extract all the mixins from the Agent jar and add them to the bootstrap classloader. JAVA-609.
     * <p>
     * Implementation note: In addition to the mixin interfaces themselves, a small number of dependent classes are
     * extracted into the generated jar and loaded on the bootstrap. These are marked with the LoadOnBootstrap
     * annotation. One class so marked is the InterfaceMixin annotation class itself.
     * <p>
     * This method duplicates some of the functionality found in the ClassAppender class in the Weaver. This duplication
     * is intentional. This code runs before bootstrap classpath setup is complete. Attempting to reuse the
     * ClassAppender causes one or more classes to be loaded by the "wrong" classloader as described in the top comment
     * to this class. This could be fixed, but would invite later failures during code maintenance if dependencies were
     * re-introduced. It is safer to ensure that code used under these special conditions remains right here.
     *
     * @param agentJarResource the Agent's jar file, or a test jar file for unit testing.
     * @param agentJarUrl      the Agent's jar URL, or a test URL for unit testing.
     * @param inst             the JVM instrumentation interface, or a mock for unit testing.
     */
    public static void addMixinInterfacesToBootstrap(JarResource agentJarResource, URL agentJarUrl, Instrumentation inst) {
        boolean succeeded = false;
        final Pattern packageSearchPattern = Pattern.compile("com/newrelic/agent/instrumentation/pointcuts/(.*).class");

        // Don't be tempted to try something like this, either:
        //
        // Class<?> interfaceMixinClass = InterfaceMixin.class;
        // String interfaceMixinAnnotation = 'L' + interfaceMixinClass.getName().replace('.', '/') + ';';
        //
        // ... it will defeat our purpose by pulling class InterfaceMixin to "this" class loader. Instead:

        final String interfaceMixinAnnotation = "Lcom/newrelic/agent/instrumentation/pointcuts/InterfaceMixin;";
        final String loadOnBootstrapAnnotation = "Lcom/newrelic/agent/instrumentation/pointcuts/LoadOnBootstrap;";
        final String interfaceMapperAnnotation = "Lcom/newrelic/agent/instrumentation/pointcuts/InterfaceMapper;";
        final String methodMapperAnnotation = "Lcom/newrelic/agent/instrumentation/pointcuts/MethodMapper;";
        final String fieldAccessorAnnotation = "Lcom/newrelic/agent/instrumentation/pointcuts/FieldAccessor;";

        final List<String> bootstrapAnnotations = Arrays.asList(interfaceMixinAnnotation,
                interfaceMapperAnnotation, methodMapperAnnotation, fieldAccessorAnnotation, loadOnBootstrapAnnotation);

        File generatedFile = null;
        JarOutputStream outputJarStream = null;
        try {
            generatedFile = File.createTempFile(NEWRELIC_BOOTSTRAP, ".jar", BootstrapLoader.getTempDir());
            Manifest manifest = createManifest();
            outputJarStream = createJarOutputStream(generatedFile, manifest);
            long modTime = System.currentTimeMillis();

            Collection<String> fileNames = AgentJarHelper.findJarFileNames(agentJarUrl, packageSearchPattern);
            for (String fileName : fileNames) {
                int size = (int) agentJarResource.getSize(fileName);
                ByteArrayOutputStream out = new ByteArrayOutputStream(size);
                Streams.copy(agentJarResource.getInputStream(fileName), out, size, true);
                byte[] classBytes = out.toByteArray();

                ClassReader cr = new ClassReader(classBytes);
                ClassStructure structure = ClassStructure.getClassStructure(cr, ClassStructure.CLASS_ANNOTATIONS);
                Collection<String> annotations = structure.getClassAnnotations().keySet();
                if (containsAnyOf(bootstrapAnnotations, annotations)) {
                    JarEntry entry = new JarEntry(fileName);
                    entry.setTime(modTime);
                    outputJarStream.putNextEntry(entry);
                    outputJarStream.write(classBytes);
                }
            }

            outputJarStream.closeEntry();
            succeeded = true;
        } catch (IOException iox) {
            logIfNRDebug("generating mixin jar file", iox);
        } finally {
            try {
                outputJarStream.close();
            } catch (Throwable th) {
                logIfNRDebug("closing outputJarStream", th);
            }
        }

        if (succeeded) {
            // And finally, tada ...
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(generatedFile);
                inst.appendToBootstrapClassLoaderSearch(jarFile);
                generatedFile.deleteOnExit();
            } catch (IOException iox) {
                logIfNRDebug("adding dynamic mixin jar to bootstrap", iox);
            } finally {
                try {
                    jarFile.close();
                } catch (Throwable th) {
                    logIfNRDebug("closing generated jar file", th);
                }
            }
        }
    }

    // Return true if the second collection contains any member of the first collection.
    private static final boolean containsAnyOf(Collection<?> searchFor, Collection<?> searchIn) {
        for (Object key : searchFor) {
            if (searchIn.contains(key)) {
                return true;
            }
        }
        return false;
    }

    // The "newrelic.disable.mixins.on.bootstrap" flag prevents us from dynamically generating the
    // mixin interface jar file and placing it on the bootstrap classloader path. This restores
    // the "old" 3.x Agent behavior that was in effect from 3.0.x through 3.12.x. Again, since we
    // haven't initialized the Agent we cannot use the standard AgentConfig for this.
    private static final boolean isDisableMixinsOnBootstrap() {
        String newrelicDisableMixinsOnBootstrap = "newrelic.disable.mixins.on.bootstrap";
        return System.getProperty(newrelicDisableMixinsOnBootstrap) != null
                && Boolean.getBoolean(newrelicDisableMixinsOnBootstrap);
    }

    // Use of this method should be limited to serious error cases that would cause the Agent to
    // shut down if not caught.
    private static final void logIfNRDebug(String msg, Throwable th) {
        if (isDebugEnabled()) {
            System.out.println("While bootstrapping the Agent: " + msg + ": " + th.getStackTrace());
        }
    }

    private static final JarOutputStream createJarOutputStream(File jarFile, Manifest manifest) throws IOException {
        FileOutputStream outStream = new FileOutputStream(jarFile);
        return new java.util.jar.JarOutputStream(outStream, manifest);
    }

    private static final Manifest createManifest() {
        Manifest manifest = new Manifest();
        Attributes a = manifest.getMainAttributes();
        a.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        a.put(Attributes.Name.IMPLEMENTATION_TITLE, "Interface Mixins");
        a.put(Attributes.Name.IMPLEMENTATION_VERSION, "1.0");
        a.put(Attributes.Name.IMPLEMENTATION_VENDOR, "New Relic");
        return manifest;
    }

}
