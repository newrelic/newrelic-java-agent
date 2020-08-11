package com.newrelic.agent.discovery;

import java.io.File;
import java.io.PrintStream;
import java.net.URISyntaxException;

public class SafeDiscovery {

    /**
     * This reflectively loads tools.jar classes so that we can provide a better error
     * message to customers if they don't have the classpath properly configured.
     */
    public static void discoverAndAttach(AttachOptions options) {
        checkToolsJar();
        Discovery.discover(options);
    }

    static void checkToolsJar() {
        try {
            String[] toolsClasses = new String[] {"sun.misc.VMSupport", "com.sun.tools.attach.VirtualMachine"};
            for (String className : toolsClasses) {
                SafeDiscovery.class.getClassLoader().loadClass(className);
            }
        } catch (ClassNotFoundException| IllegalArgumentException | SecurityException e) {
            try {
                printHelp(System.out, System.err);
            } catch (URISyntaxException e1) {
            }
            throw new RuntimeException(e);
        }
    }

    static void printHelp(PrintStream out, PrintStream err) throws URISyntaxException {
        err.println("Run this command using a Java 8 JDK with tools.jar on the classpath");
        final String agentJar = Discovery.getAgentJarPath();
        out.println("\tjava -cp " + agentJar + File.pathSeparatorChar +
                "${JAVA_HOME}/lib/tools.jar com.newrelic.bootstrap.BootstrapAgent attach");
    }
}
