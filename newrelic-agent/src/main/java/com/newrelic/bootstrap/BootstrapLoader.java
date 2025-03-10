/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.bootstrap;

import com.newrelic.api.agent.security.NewRelicSecurity;
import io.opentelemetry.javaagent.OpenTelemetryAgent;
import io.opentelemetry.javaagent.shaded.instrumentation.api.instrumenter.InstrumenterBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads the agent-bridge, newrelic-api, and newrelic-bootstrap jars onto the bootstrap classpath.
 * <p>
 * Warning to maintainers: even slight changes to the code in this class can cause subtle failures at Agent startup
 * time. The methods in this class modify the bootstrap classpath. Any change that causes bootstrap classes to be
 * loaded before the bootstrap classpath changes are complete will result in said classes being loaded by the "wrong"
 * classloader, potentially causing peculiar consequences later in execution. The existing dependencies (AgentJarHelper,
 * JarResource, asm.ClassStructure) have been checked and don't result in undesirable classes being pulled into the
 * "wrong" loader. Changes to dependencies must be understood and tested carefully. Dependencies on java.* are "safe".
 * But in particular, do not introduce a dependency on the Logger here (it doesn't help anything, anyway, because the
 * Logger has not been configured at this point in Agent startup).
 */
public class BootstrapLoader {

    public static final String AGENT_BRIDGE_JAR_NAME = "agent-bridge";
    public static final String AGENT_BRIDGE_DATASTORE_JAR_NAME = "agent-bridge-datastore";

    /**
     * The tempdir setting is not prefixed with `config` because it can't be set through the config file,
     * only as a system property.
     */
    private static final String NEWRELIC_TEMPDIR = "newrelic.tempdir";

    public static final String API_JAR_NAME = "newrelic-api";

    public static final String WEAVER_API_JAR_NAME = "newrelic-weaver-api";

    public static final String NEWRELIC_SECURITY_AGENT = "newrelic-security-agent";

    public static final String NEWRELIC_SECURITY_API = "newrelic-security-api";


    public static final String OPENTELEMETRY_JAVAAGENT_TOOLING = "opentelemetry-javaagent-tooling-2.12.0-alpha";

    public static final String OPENTELEMETRY_JAVAAGENT_BOOTSTRAP = "opentelemetry-javaagent-bootstrap-2.12.0-alpha";
    public static final String OPENTELEMETRY_JAVAAGENT = "opentelemetry-javaagent-2.12.0";


    static final class ApiClassTransformer implements ClassFileTransformer {
        private final byte[] bytes;
        private final String apiClassName;

        ApiClassTransformer(String apiClassName, byte[] bytes) {
            this.bytes = bytes;
            this.apiClassName = apiClassName;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

            if (className == null) {
                return null;
            }

            if (apiClassName.equals(className)) {
                return bytes;
            }
            return null;
        }
    }

    private static final String NEWRELIC_API_INTERNAL_CLASS_NAME = "com/newrelic/api/agent/NewRelic";
    private static final String NEWRELIC_SECURITY_API_INTERNAL_CLASS_NAME = "com/newrelic/api/agent/security/NewRelicSecurity";

    private static final String OPENTELEMETRY_AGENT_INTERNAL_CLASS_NAME = "io/opentelemetry/javaagent/OpenTelemetryAgent";

    private static void addBridgeJarToClassPath(Instrumentation instrProxy, String jar) throws ClassNotFoundException, IOException {
        JarFile jarFileInAgent = new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(jar));
        addJarToClassPath(instrProxy, jarFileInAgent);
    }

    /**
     * This forces the correct NewRelic api implementation to load by getting the implementation class bytes out of the
     * agent bridge jar and hooking up a class transformer to always load those bytes for our api class.
     */
    public static void forceCorrectNewRelicApi(Instrumentation instrProxy) throws IOException {
        JarFile bridgeJarFile = new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(AGENT_BRIDGE_JAR_NAME));
        JarEntry jarEntry = bridgeJarFile.getJarEntry(NEWRELIC_API_INTERNAL_CLASS_NAME + ".class");
        final byte[] bytes = read(bridgeJarFile.getInputStream(jarEntry), true);
        instrProxy.addTransformer(new ApiClassTransformer(NEWRELIC_API_INTERNAL_CLASS_NAME, bytes), true);
    }

    /**
     * This forces the correct NewRelic Security api implementation to load by getting the implementation class bytes out of the
     * security agent jar and hooking up a class transformer to always load those bytes for our api class.
     */
    public static void forceCorrectNewRelicSecurityApi(Instrumentation instrProxy) throws IOException, UnmodifiableClassException {
        JarFile securityAgentJarFile = new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(NEWRELIC_SECURITY_AGENT));
        JarEntry jarEntry = securityAgentJarFile.getJarEntry(NEWRELIC_SECURITY_API_INTERNAL_CLASS_NAME + ".class");
        final byte[] bytes = read(securityAgentJarFile.getInputStream(jarEntry), true);
        instrProxy.addTransformer(new ApiClassTransformer(NEWRELIC_SECURITY_API_INTERNAL_CLASS_NAME, bytes), true);
        instrProxy.retransformClasses(NewRelicSecurity.class);
    }

    public static void forceCorrectOpenTelemetryApi(Instrumentation instrProxy) throws IOException, UnmodifiableClassException {
        JarFile openTelemetryAgentJarFile = new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(OPENTELEMETRY_JAVAAGENT));
        JarEntry jarEntry = openTelemetryAgentJarFile.getJarEntry(OPENTELEMETRY_AGENT_INTERNAL_CLASS_NAME + ".class");
        final byte[] bytes = read(openTelemetryAgentJarFile.getInputStream(jarEntry), true);
        // FIXME do we even need to transform this OpenTelemetryAgent class?
        instrProxy.addTransformer(new ApiClassTransformer(OPENTELEMETRY_AGENT_INTERNAL_CLASS_NAME, bytes), true);
        instrProxy.retransformClasses(OpenTelemetryAgent.class);

        // InstrumenterBuilder
//        JarEntry jarEntry2 = openTelemetryAgentJarFile.getJarEntry("io/opentelemetry/javaagent/shaded/instrumentation/api/instrumenter/InstrumenterBuilder" + ".class");
//        final byte[] bytes2 = read(openTelemetryAgentJarFile.getInputStream(jarEntry2), true);
//        instrProxy.addTransformer(new ApiClassTransformer("io/opentelemetry/javaagent/shaded/instrumentation/api/instrumenter/InstrumenterBuilder", bytes2), true);
//        instrProxy.retransformClasses(InstrumenterBuilder.class);


//        JarFile openTelemetryBootstrapJarFile = new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(OPENTELEMETRY_JAVAAGENT_BOOTSTRAP));
//        JarFile openTelemetryToolingJarFile = new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(OPENTELEMETRY_JAVAAGENT_TOOLING));
    }

    private static void addJarToClassPath(Instrumentation instrProxy, JarFile jarfile) {
        instrProxy.appendToBootstrapClassLoaderSearch(jarfile);
    }

    public static URL getDatastoreJarURL() throws ClassNotFoundException, IOException {
        File jarFileInAgent = EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(AGENT_BRIDGE_DATASTORE_JAR_NAME);
        return jarFileInAgent.toURI().toURL();
    }

    /**
     * Returns URLs for the jars contained within the agent jar. These urls point to temp files created by writing the
     * embedded jars out to disk. This method is used to ensure visibility of these embedded jars when the Agent is run
     * from the command line. This code is not used when we initialize as an Agent via premain().
     *
     * @return URLs of certain jars embedded within the Agent jar
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Collection<URL> getJarURLs() throws ClassNotFoundException, IOException {
        List<URL> urls = new ArrayList<>();
        for (String name : new String[] { AGENT_BRIDGE_JAR_NAME, AGENT_BRIDGE_DATASTORE_JAR_NAME,
                API_JAR_NAME, WEAVER_API_JAR_NAME, NEWRELIC_SECURITY_AGENT, NEWRELIC_SECURITY_API }) {
            File jarFileInAgent = EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(name);
            urls.add(jarFileInAgent.toURI().toURL());
        }
        return urls;
    }

    /**
     * Primary interface to this class. Manipulate class paths as required when we run as an Agent.
     *
     * @param inst                                 the instrumentation interface to JVM
     * @param isJavaSqlLoadedOnPlatformClassLoader true if java.sql is loaded by the subordinate
     *                                             platform class loader. If so, we can't add the datastore jar to the bootstrap because
     *                                             required classes will be loaded by the platform class loader instead.
     */
    static void load(Instrumentation inst, boolean isJavaSqlLoadedOnPlatformClassLoader) {
        try {
            if (!isJavaSqlLoadedOnPlatformClassLoader) {
                addBridgeJarToClassPath(inst, AGENT_BRIDGE_DATASTORE_JAR_NAME);
            }

            addBridgeJarToClassPath(inst, AGENT_BRIDGE_JAR_NAME);
            addJarToClassPath(inst, new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(API_JAR_NAME)));
            addJarToClassPath(inst, new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(WEAVER_API_JAR_NAME)));
            addJarToClassPath(inst, new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(NEWRELIC_SECURITY_API)));
            addJarToClassPath(inst, new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(NEWRELIC_SECURITY_AGENT)));
//            inst.appendToSystemClassLoaderSearch(new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(OPENTELEMETRY_JAVAAGENT)));
            addJarToClassPath(inst, new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(OPENTELEMETRY_JAVAAGENT)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copy bytes from an InputStream to an OutputStream.
     *
     * @param input  the InputStream to read from
     * @param output the OutputStream to write to
     * @return the number of bytes copied
     * @throws IOException In case of an I/O problem
     */
    static int copy(InputStream input, OutputStream output, int bufferSize, boolean closeStreams) throws IOException {
        try {
            byte[] buffer = new byte[bufferSize];
            int count = 0;
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
            return count;
        } finally {
            if (closeStreams) {
                input.close();
                output.close();
            }
        }
    }

    static byte[] read(InputStream input, boolean closeInputStream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        copy(input, outStream, input.available(), closeInputStream);
        return outStream.toByteArray();
    }

    /**
     * Returns the tempdir that the agent should use, or null if the default temp directory should
     * be used.  This can be set using the newrelic.tempdir system property.
     */
    public static File getTempDir() {
        String tempDir = System.getProperty(NEWRELIC_TEMPDIR);
        if (null != tempDir) {
            File tempDirFile = new File(tempDir);
            if (tempDirFile.exists()) {
                return tempDirFile;
            } else {
                System.err.println("Temp directory specified by newrelic.tempdir does not exist - " + tempDir);
            }
        }
        return null;
    }

}
