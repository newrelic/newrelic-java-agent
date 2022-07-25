/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.bootstrap;

import com.newrelic.agent.config.IBMUtils;
import com.newrelic.agent.config.JavaVersionUtils;
import com.newrelic.agent.modules.ClassLoaderUtil;
import com.newrelic.agent.modules.ClassLoaderUtilImpl;
import com.newrelic.agent.modules.HttpModuleUtil;
import com.newrelic.agent.modules.HttpModuleUtilImpl;
import com.newrelic.agent.modules.ModuleUtil;
import com.newrelic.agent.modules.ModuleUtilImpl;
import org.apache.commons.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.zip.InflaterInputStream;

public class BootstrapAgent {

    public static final String TRY_IBM_ATTACH_SYSTEM_PROPERTY = "newrelic.try_ibm_attach";
    public static final String NR_AGENT_ARGS_SYSTEM_PROPERTY = "nr-internal-agent-args";
    private static final String AGENT_CLASS_NAME = "com.newrelic.agent.Agent";
    private static final String JAVA_LOG_MANAGER = "java.util.logging.manager";
    private static final String WS_SERVER_JAR = "ws-server.jar";
    private static final String WS_LOG_MANAGER = "com.ibm.ws.kernel.boot.logging.WsLogManager";

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
        byte[] decodeBase64 = Base64.decodeBase64(agentArgs);
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
        String sysExperimentalRuntime = System.getProperty("newrelic.config.experimental_runtime");
        String envExperimentalRuntime = System.getenv("NEW_RELIC_EXPERIMENTAL_RUNTIME");
        boolean useExperimentalRuntime = (Boolean.parseBoolean(sysExperimentalRuntime)
                || ((Boolean.parseBoolean(envExperimentalRuntime))));

        if (useExperimentalRuntime) {
            System.out.println("----------");
            System.out.println(JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(javaSpecVersion));
            System.out.println("Experimental runtime mode is enabled. Usage of the agent in this mode is for experimenting with early access" +
                    " or upcoming Java releases or at your own risk.");
            System.out.println("----------");
        }
        if (!JavaVersionUtils.isAgentSupportedJavaSpecVersion(javaSpecVersion) && !useExperimentalRuntime) {
            System.err.println("----------");
            System.err.println(JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(javaSpecVersion));
            System.err.println("----------");
            return;
        }

        String sysPropEnabled = System.getProperty("newrelic.config.agent_enabled");
        String envVarEnabled = System.getenv("NEW_RELIC_AGENT_ENABLED");
        if (sysPropEnabled != null && !Boolean.parseBoolean(sysPropEnabled)) {
            System.err.println("----------");
            System.err.println("New Relic Agent is disabled by -Dnewrelic.config.agent_enabled system property.");
            System.err.println("----------");
            return;
        } else if (envVarEnabled != null && !Boolean.parseBoolean(envVarEnabled)) {
            System.err.println("----------");
            System.err.println("New Relic Agent is disabled by NEW_RELIC_AGENT_ENABLED environment variable.");
            System.err.println("----------");
            return;
        }

        checkAndApplyIBMLibertyProfileLogManagerWorkaround();
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

            // Check for the IBM workaround
            boolean ibmWorkaround = IBMUtils.getIbmWorkaroundDefault();
            if (System.getProperty("ibm_iv25688_workaround") != null) {
                ibmWorkaround = Boolean.parseBoolean(System.getProperty("ibm_iv25688_workaround"));
            }

            ClassLoader classLoader;
            if (ibmWorkaround) {
                // For the IBM workaround lets just use the System ClassLoader
                classLoader = ClassLoader.getSystemClassLoader();
            } else {
                ClassLoader agentClassLoaderParent = getPlatformClassLoaderOrNull();

                // Create a new URLClassLoader instance for the agent to use instead of relying on the System ClassLoader
                URL[] codeSource;
                if (isJavaSqlLoadedOnPlatformClassLoader(javaVersion)) {
                    URL url = BootstrapLoader.getDatastoreJarURL();
                    codeSource = new URL[] { getAgentJarUrl(), url };
                } else {
                    codeSource = new URL[] { getAgentJarUrl() };
                }

                classLoader = new JVMAgentClassLoader(codeSource, agentClassLoaderParent);

                redefineJavaBaseModule(inst, classLoader);
                addReadUnnamedModuleToHttpModule(inst, agentClassLoaderParent);
            }

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
     * by default.
     *
     * @param javaVersion the "java.version" system property.
     * @return true if java.sql classes will be loaded by the platform class loader.
     */
    private static boolean isJavaSqlLoadedOnPlatformClassLoader(String javaVersion) {
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

}
