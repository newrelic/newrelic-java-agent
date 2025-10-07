/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.bootstrap;

import com.newrelic.agent.config.IBMUtils;
import com.newrelic.agent.config.JavaVersionUtils;
import com.newrelic.agent.config.JbossUtils;
import com.newrelic.agent.modules.ClassLoaderUtil;
import com.newrelic.agent.modules.ClassLoaderUtilImpl;
import com.newrelic.agent.modules.HttpModuleUtil;
import com.newrelic.agent.modules.HttpModuleUtilImpl;
import com.newrelic.agent.modules.ModuleUtil;
import com.newrelic.agent.modules.ModuleUtilImpl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.zip.InflaterInputStream;

public class BootstrapAgent {

    public static final String TRY_IBM_ATTACH_SYSTEM_PROPERTY = "newrelic.try_ibm_attach";
    public static final String NR_AGENT_ARGS_SYSTEM_PROPERTY = "nr-internal-agent-args";
    private static final String AGENT_CLASS_NAME = "com.newrelic.agent.Agent";
    private static final String JAVA_LOG_MANAGER = "java.util.logging.manager";
    private static final String WS_SERVER_JAR = "ws-server.jar";
    private static final String WS_LOG_MANAGER = "com.ibm.ws.kernel.boot.logging.WsLogManager";
    private static final String AGENT_ENABLED_ENV_VAR = "NEW_RELIC_AGENT_ENABLED";
    private static final String AGENT_ENABLED_SYS_PROP = "newrelic.config.agent_enabled";
    private static final String STARTUP_JAVA_ARTIFACT_SKIPS_ENV_VAR = "NEW_RELIC_STARTUP_JAVA_ARTIFACT_SKIPS";
    private static final String STARTUP_JAVA_ARTIFACT_SKIPS_SYS_PROP = "newrelic.config.startup_java_artifact_skips";
    private static final String SQL_ON_PLATFORM_LOADER_SYS_PROP = "newrelic.config.sql.platformClassloader";

    public static URL getAgentJarUrl() {
        return BootstrapAgent.class.getProtectionDomain().getCodeSource().getLocation();
    }

    /**
     * A wrapper around the Agent's main method that makes sure the bootstrap classes are available.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            Collection<URL> urls = BootstrapLoader.getJarURLs();
            urls.add(getAgentJarUrl());
            @SuppressWarnings("resource")
            ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), null);
            Class<?> agentClass = classLoader.loadClass(AGENT_CLASS_NAME);
            Method main = agentClass.getDeclaredMethod("main", String[].class);
            main.invoke(null, new Object[] { args });
        } catch (Throwable t) {
            System.err.println(MessageFormat.format("Error invoking the New Relic command: {0}", t));
            t.printStackTrace();
        }
    }

    /**
     * This is invoked when the agent is attached to a running process.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        if (agentArgs == null || agentArgs.isEmpty()) {
            throw new IllegalArgumentException("Unable to attach. The license key was not specified");
        }
        System.out.println("Attaching the New Relic java agent");
        // force this formatter to load early to avoid a java.lang.ClassCircularityError
        MessageFormat.format("{0}", 1.0);
        try {
            agentArgs = decodeAndDecompressAgentArguments(agentArgs);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.setProperty(NR_AGENT_ARGS_SYSTEM_PROPERTY, agentArgs);
        premain(agentArgs, inst);
    }

    static String decodeAndDecompressAgentArguments(String agentArgs) throws IOException {
        byte[] decodeBase64 = Base64.getDecoder().decode(agentArgs);
        InflaterInputStream zipStream = new InflaterInputStream(new ByteArrayInputStream(decodeBase64));
        return new BufferedReader(new InputStreamReader(zipStream)).readLine();
    }

    /**
     * This is called via the Java 1.5 Instrumentation startup sequence (JSR 163). Boot up the agent.
     * <p>
     * Thanks Mr. Cobb! ;)
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        String javaSpecVersion = JavaVersionUtils.getJavaSpecificationVersion();
        if (useExperimentalRuntime()) {
            printExperimentalRuntimeModeInUseMessage(javaSpecVersion);
        }
        if (!JavaVersionUtils.isAgentSupportedJavaSpecVersion(javaSpecVersion) && !useExperimentalRuntime()) {
            printUnsupportedJavaVersionMessage(javaSpecVersion);
            return;
        }
        if (agentIsDisabledBySystemPropertyOrEnvVar()) {
            printAgentIsDisabledBySysPropOrEnvVar();
            return;
        }
        if (isAgentDisabledByStartupJavaArtifactSkip(javaSpecVersion)) {
            printAgentIsDisabledByArtifactSkip();
            return;
        }

        checkAndApplyIBMLibertyProfileLogManagerWorkaround();
        new JbossUtils().checkAndApplyJbossAdjustments(inst);
        startAgent(agentArgs, inst);
    }

    private static void checkAndApplyIBMLibertyProfileLogManagerWorkaround() {
        if (IBMUtils.isIbmJVM()) {
            String javaClassPath = System.getProperty("java.class.path");
            // WS_SERVER_JAR is characteristic of a Liberty Profile installation
            if (javaClassPath != null && javaClassPath.contains(WS_SERVER_JAR)) {
                if (System.getProperty(JAVA_LOG_MANAGER) == null) {
                    try {
                        // Check for the existence of WsLogManager (without initializing it)
                        // before attempting the logging manager workaround
                        Class.forName(WS_LOG_MANAGER, false, BootstrapAgent.class.getClassLoader());

                        // This property is used in java.util.logging.LogManager during initialization
                        // and allows us to properly set the logger hierarchy for Liberty Profile.
                        System.setProperty(JAVA_LOG_MANAGER, WS_LOG_MANAGER);
                    } catch (Exception e) {
                        // WSLogManager was not found, this must not be Liberty
                    }
                }
            }
        }
    }

    private static void startAgent(String agentArgs, Instrumentation inst) {
        try {
            // Premain start time will be recorded starting from this point
            long startTime = System.currentTimeMillis();

            String javaVersion = System.getProperty("java.version", "");
            BootstrapLoader.load(inst, isJavaSqlLoadedOnPlatformClassLoader(javaVersion));

            ClassLoader agentClassLoaderParent = getPlatformClassLoaderOrNull();

            // Create a new URLClassLoader instance for the agent to use instead of relying
            // on the System ClassLoader (aka and now called Application ClassLoader)
            URL[] codeSource;
            if (isJavaSqlLoadedOnPlatformClassLoader(javaVersion)) {
                // Java 9+ we haven't added the agent-bridge-datastore.jar to the classpath yet
                // because java.sql is loaded by the platform loader, so we skipped loading agent-bridge-datastore for Java 9+ versions.
                // We now need to give the agent-bridge-datastore url and the platform classloader (as the parent classloader).
                URL url = BootstrapLoader.getDatastoreJarURL();
                codeSource = new URL[] { getAgentJarUrl(), url };
            } else {
                // agent-bridge-datastore.jar was already added to the classpath by the System/App classloader via BootstrapLoader
                codeSource = new URL[] { getAgentJarUrl() };
            }
            // When we have come through the above 'else' path (java versions < 9) the agentClassLoaderParent will be null. This is okay
            // because the url provides the jar and all the agent classes. The weaver, agent-api, agent-bridge, and agent-datastore
            // jars have already been added to the classpath and loaded by the com.newrelic.BootstrapLoader (which is loaded by AppClassLoader) for this case.
            ClassLoader classLoader = new JVMAgentClassLoader(codeSource, agentClassLoaderParent);

            redefineJavaBaseModule(inst, classLoader);
            addReadUnnamedModuleToHttpModule(inst, agentClassLoaderParent);

            Class<?> agentClass = classLoader.loadClass(AGENT_CLASS_NAME);
            Method continuePremain = agentClass.getDeclaredMethod("continuePremain", String.class, Instrumentation.class, long.class);
            continuePremain.invoke(null, agentArgs, inst, startTime);
        } catch (Throwable t) {
            System.err.println(MessageFormat.format("Error bootstrapping New Relic agent: {0}", t));
            t.printStackTrace();
        }
    }

    /**
     * The "getPlatformClassLoader" method only exists on Java >= 9, so we reflect in to get it. If there's no
     * platform class loader (usually because we're on Java < 9 and the concept doesn't exist), then
     * we return null.
     *
     * @return the platform class loader on Java >= 9; null otherwise.
     */
    private static ClassLoader getPlatformClassLoaderOrNull() {
        try {
            ClassLoaderUtil util = new ClassLoaderUtilImpl();
            return util.getPlatformClassLoaderOrNull();
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Modify the java.base module so that reflective/MethodHandle access from agent code
     * can get past the module access controls.
     *
     * <p>{@link ModuleUtil} is compiled in a multi-release jar. In Java &lt; 9, this
     * results in a no-op implementation.</p>
     *
     * @param inst             The premain {@link Instrumentation} interface.
     * @param agentClassLoader The class loader used for loading agent classes.
     */
    private static void redefineJavaBaseModule(Instrumentation inst, ClassLoader agentClassLoader) {
        try {
            ModuleUtil util = new ModuleUtilImpl();
            util.redefineJavaBaseModule(inst, agentClassLoader);
        } catch (Throwable t) {
            System.err.println("The agent failed to redefine modules as necessary. " + t);
        }
    }

    /**
     * Modify the java.net.http module so that it can read from the platform classloader's
     * unnamed module. The agent http client instrumentation utility classes are
     * in this specific unnamed module.
     *
     * <p>{@link ModuleUtil} is compiled in a multi-release jar. In Java &lt; 11, this
     * results in a no-op implementation.</p>
     *
     * @param inst                The premain {@link Instrumentation} interface.
     * @param platformClassLoader
     */
    private static void addReadUnnamedModuleToHttpModule(Instrumentation inst, ClassLoader platformClassLoader) {
        try {
            HttpModuleUtil util = new HttpModuleUtilImpl();
            util.addReadHttpModule(inst, platformClassLoader);
        } catch (Throwable t) {
            System.err.println("The agent failed to redefine modules as necessary. " + t);
        }
    }

    /**
     * Indicates that java.sql classes are not included on the bootstrap class path
     * by default. This usually depends on the JVM version, but some app servers like
     * to do things differently, and the agent has no way to know that.
     *
     * @param javaVersion the "java.version" system property.
     * @return true if java.sql classes will be loaded by the platform class loader.
     */
    private static boolean isJavaSqlLoadedOnPlatformClassLoader(String javaVersion) {
        String config = System.getProperty(SQL_ON_PLATFORM_LOADER_SYS_PROP);
        if (config != null) {
            return Boolean.parseBoolean(config);
        }
        return !javaVersion.startsWith("1.");
    }

    private static class JVMAgentClassLoader extends URLClassLoader {
        static {
            try {
                registerAsParallelCapable();
            } catch (Throwable t) {
                System.err.println(MessageFormat.format("Unable to register as parallel-capable: {0}", t));
            }
        }

        public JVMAgentClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
    }

    /**
     * This allows applications to be selectively skipped based on the startup main class or executable
     * jar file extracted from the command line. This is handy in situations where the "JAVA_TOOL_OPTIONS"
     * environment variable contains the -javaagent flag but we don't want to apply the instrumentation
     * to all java apps in the environment.
     *
     * @param javaSpecVersion the version String of the JRE in use
     *
     * @return true if the agent should not be enabled for this application
     */
    private static boolean isAgentDisabledByStartupJavaArtifactSkip(String javaSpecVersion) {
        String [] javaArtifactSkipList = parseStartupJavaArtifactSkips();

        if (javaArtifactSkipList != null) {
            String startupJavaArtifact = getStartupJavaArtifact(javaSpecVersion);
            System.out.println("New Relic Agent: Configured startup Java artifacts skip string: " + String.join(",", javaArtifactSkipList));
            System.out.println("New Relic Agent: Retrieved current startup command line / main artifact name: " + (startupJavaArtifact == null ? "null" : startupJavaArtifact));
            if (startupJavaArtifact != null) {
                for (String skip : javaArtifactSkipList) {
                    if (startupJavaArtifact.contains(skip)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Extract the startup command line / startup artifact name.<br>
     * The ProcessHandle method only works on Java 9 and above. Using the ProcessHandle,
     * attempt to get the command line used to start the current Java process.
     * Reflection is required since we might be running on Java 8 where these classes
     * don't exist. The actual code to get this would be:<br>
     *     Optional<String> commandLine = ProcessHandle.current().info().commandLine();
     *
     * @param javaSpecVersion the version String of the running JRE
     *
     * @return the startup command line if available
     */
    private static String getStartupJavaArtifact(String javaSpecVersion) {
        if (!javaSpecVersion.equals("1.8")) {
            try {
                Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
                Method infoMethod = processHandleClass.getDeclaredMethod("info");
                Method currentMethod = processHandleClass.getMethod("current");

                Class<?> processHandleInfoClass = Class.forName("java.lang.ProcessHandle$Info");
                Method commandLineMethod = processHandleInfoClass.getDeclaredMethod("commandLine");

                Object currentObject = currentMethod.invoke(null);
                Object infoObject =  infoMethod.invoke(currentObject);
                Object commandLineObject = commandLineMethod.invoke(infoObject);

                if (commandLineObject instanceof Optional) {
                    Optional optionalInstance = (Optional) commandLineObject;
                    if (optionalInstance.isPresent()) {
                        return optionalInstance.get().toString();
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // Running on Java 1.8
        // This environment variable MIGHT exist, but it's not a guarantee
        return System.getProperty("sun.java.command");
    }

    /**
     * Extract the defined jars/classes to skip.<br>
     * The skip list is configured via the `NEW_RELIC_STARTUP_JAVA_ARTIFACT_SKIPS_ENV_VAR` environment variable
     * or the newrelic.config.startup_java_artifact_skips system property.
     * This is a comma separated list of main classes, executable jar files or Java based tools/apps
     * that the agent should NOT instrument. For example:<br>
     * export NEW_RELIC_STARTUP_JAVA_ARTIFACT_SKIPS=keytool,myapp.jar,IgnoreThisClass
     * <br>
     * @return a String [] of defined skip tokens
     */
    private static String [] parseStartupJavaArtifactSkips() {
        String envVal = System.getenv(STARTUP_JAVA_ARTIFACT_SKIPS_ENV_VAR);
        String sysVal = System.getProperty(STARTUP_JAVA_ARTIFACT_SKIPS_SYS_PROP);

        if ((envVal != null && !envVal.isEmpty())) {
            return envVal.split(",");
        } else if ((sysVal != null && !sysVal.isEmpty())) {
            return sysVal.split(",");
        }

        return null;
    }

    private static boolean agentIsDisabledBySystemPropertyOrEnvVar() {
        String sysVal = System.getProperty(AGENT_ENABLED_SYS_PROP);
        String envVal = System.getenv(AGENT_ENABLED_ENV_VAR);
        // We also check for null here because we only want to know if
        // if false is explicitly set for either value. Otherwise, null from getProperty would cause
        // parseBoolean to return a false negative.
        return (sysVal != null && !Boolean.parseBoolean(sysVal)) ||
                (envVal != null && !Boolean.parseBoolean(envVal));
    }

    private static void printAgentIsDisabledByArtifactSkip() {
        System.err.println("----------");
        System.err.println("New Relic Agent is disabled by startup class/jar skip configuration.");
        System.err.println("----------");
    }


    private static void printAgentIsDisabledBySysPropOrEnvVar() {
        System.err.println("----------");
        System.err.println(MessageFormat.format("New Relic Agent is disabled by {0} system property" +
                " or {1} environment variable.", AGENT_ENABLED_SYS_PROP, AGENT_ENABLED_ENV_VAR));
        System.err.println("----------");
    }

    private static boolean useExperimentalRuntime() {
        return Boolean.parseBoolean(System.getProperty("newrelic.config.experimental_runtime"))
                || Boolean.parseBoolean(System.getenv("NEW_RELIC_EXPERIMENTAL_RUNTIME"));
    }

    private static void printExperimentalRuntimeModeInUseMessage(String javaSpecVersion) {
        System.out.println("----------");
        System.out.println(JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(javaSpecVersion));
        System.out.println("Experimental runtime mode is enabled. Usage of the agent in this mode is for experimenting with early access" +
                " or upcoming Java releases at your own risk.");
        System.out.println("----------");
    }

    private static void printUnsupportedJavaVersionMessage(String javaSpecVersion) {
        System.err.println("----------");
        System.err.println(JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(javaSpecVersion));
        System.err.println("----------");
    }

}
