package com.newrelic.agent.discovery;

import java.io.File;
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

    private static void checkToolsJar() {
        try {
            String[] toolsClasses = new String[] {"sun.misc.VMSupport", "com.sun.tools.attach.VirtualMachine"};
            for (String className : toolsClasses) {
                SafeDiscovery.class.getClassLoader().loadClass(className);
            }
        } catch (ClassNotFoundException| IllegalArgumentException | SecurityException e) {
            try {
                printHelp();
            } catch (URISyntaxException e1) {
            }
            throw new RuntimeException(e);
        }
    }

    private static void printHelp() throws URISyntaxException {
        System.err.println("Run this command using a Java 8 JDK with tools.jar on the classpath");
        final String agentJar = Discovery.getAgentJarPath();
        System.out.println("\tjava -cp " + agentJar + File.pathSeparatorChar +
                "${JAVA_HOME}/lib/tools.jar com.newrelic.bootstrap.BootstrapAgent attach");
    }
}
