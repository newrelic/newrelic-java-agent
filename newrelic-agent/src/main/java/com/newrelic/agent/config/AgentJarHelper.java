/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.DebugFlag;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

public class AgentJarHelper {

    private static final String AGENT_CLASS_NAME = "com/newrelic/agent/Agent.class";
    private static final String NEW_RELIC_JAR_FILE = "newrelic.jar";
    private static final String BUILT_DATE_ATTRIBUTE = "Built-Date";

    /**
     * Find all the file names in the agent jar that match the given pattern.
     */
    public static Collection<String> findAgentJarFileNames(Pattern pattern) {
        URL agentJarUrl = getAgentJarUrl();
        if (isNewRelicDebug()) {
            System.out.println("Searching for " + pattern.pattern() + " in " + agentJarUrl.getPath());
        }
        return findJarFileNames(agentJarUrl, pattern);
    }

    public static Collection<String> findJarFileNames(URL agentJarUrl, Pattern pattern) {
        try (JarFile jarFile = getAgentJarFile(agentJarUrl)) {

            Collection<String> names = new ArrayList<>();
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                JarEntry jarEntry = entries.nextElement();
                if (pattern.matcher(jarEntry.getName()).matches()) {
                    names.add(jarEntry.getName());
                }
            }
            return names;
        } catch (Exception e) {
            logIfNRDebug("Unable to search the agent jar for " + pattern.pattern(), e);
        }
        return Collections.emptyList();
    }

    public static boolean jarFileNameExists(URL agentJarUrl, String name) {
        try (JarFile jarFile = getAgentJarFile(agentJarUrl)) {
            return jarFile.getEntry(name) != null;
        } catch (Exception e) {
            logIfNRDebug("Unable to search the agent jar for " + name, e);
        }
        return false;
    }

    /**
     * Get the directory containing newrelic.jar.
     *
     * @return the directory containing newrelic.jar or null
     */
    public static File getAgentJarDirectory() {
        URL agentJarUrl = getAgentJarUrl();
        if (agentJarUrl != null) {
            File file = new File(getAgentJarFileName(agentJarUrl));
            if (file.exists()) {
                return file.getParentFile();
            }
        }
        return null;
    }

    public static URL getAgentJarUrl() {
        if (System.getProperty("newrelic.agent_jarfile") != null) {
            try {
                return new URL("file://" + System.getProperty("newrelic.agent_jarfile"));
            } catch (MalformedURLException e) {
                logIfNRDebug("Unable to create a valid url from " + System.getProperty("newrelic.agent_jarfile"), e);
            }
        }

        // Use AgentJarHelper's ClassLoader here because this is called from the BootstrapAgent premain
        ClassLoader classLoader = AgentJarHelper.class.getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) classLoader).getURLs();
            for (URL url : urls) {
                if (url.getFile().endsWith(NEW_RELIC_JAR_FILE)) {
                    if (jarFileNameExists(url, AGENT_CLASS_NAME)) {
                        return url;
                    }
                }
            }
            String agentClassName = AGENT_CLASS_NAME.replace('.', '/');
            for (URL url : urls) {
                try (JarFile jarFile = new JarFile(url.getFile())) {
                    ZipEntry entry = jarFile.getEntry(agentClassName);
                    if (entry != null) {
                        return url;
                    }
                } catch (IOException e) {
                }
            }
        }
        // technically this is all that is needed to get the jar URL
        // but it does require a new permission so it will be the
        // fallback method for the time being (the above approach
        // frequently fails when using a custom system class loader)
        return AgentJarHelper.class.getProtectionDomain().getCodeSource().getLocation();
    }

    /**
     * Returns a resource that can load resources from the agent jar. There are two implementations because the unit
     * tests won't find the agent jar file and in that case we find resources through the system classloader.
     */
    public static JarResource getAgentJarResource() {
        final JarFile agentJarFile = getAgentJarFile();
        if (agentJarFile == null) {
            // we should only get here in unit tests
            return new JarResource() {

                @Override
                public void close() throws IOException {
                }

                @Override
                public InputStream getInputStream(String name) {
                    return AgentJarHelper.class.getResourceAsStream('/' + name);
                }

                /**
                 * We don't know the actual size so we just return a default value.
                 */
                @Override
                public long getSize(String name) {
                    return 128;
                }
            };
        } else {
            return new JarResource() {
                @Override
                public void close() throws IOException {
                    agentJarFile.close();
                }

                @Override
                public InputStream getInputStream(String name) throws IOException {
                    ZipEntry entry = agentJarFile.getJarEntry(name);
                    return agentJarFile.getInputStream(entry);
                }

                @Override
                public long getSize(String name) {
                    ZipEntry entry = agentJarFile.getJarEntry(name);
                    return entry.getSize();
                }
            };
        }
    }

    private static JarFile getAgentJarFile() {
        URL agentJarUrl = getAgentJarUrl();
        return getAgentJarFile(agentJarUrl);
    }

    private static JarFile getAgentJarFile(URL agentJarUrl) {
        if (agentJarUrl == null) {
            return null;
        }
        try {
            return new JarFile(getAgentJarFileName(agentJarUrl));
        } catch (IOException e) {
            return null;
        }
    }

    private static String getAgentJarFileName(URL agentJarUrl) {
        if (agentJarUrl == null) {
            return null;
        }
        try {
            return URLDecoder.decode(agentJarUrl.getFile().replace("+", "%2B"), "UTF-8");
        } catch (IOException e) {
            return null;
        }
    }

    public static String getAgentJarFileName() {
        URL agentJarUrl = getAgentJarUrl();
        return getAgentJarFileName(agentJarUrl);
    }

    /**
     * Get the build date of the agent jar.
     */
    public static String getBuildDate() {
        return getAgentJarAttribute(BUILT_DATE_ATTRIBUTE);
    }

    /**
     * Return the given attribute from the agent jar's manifest.
     */
    public static String getAgentJarAttribute(String name) {
        JarFile jarFile = getAgentJarFile();
        if (jarFile == null) {
            return null;
        }
        try {
            return jarFile.getManifest().getMainAttributes().getValue(name);
        } catch (IOException e) {
            return null;
        }
    }

    private static final boolean isNewRelicDebug() {

        return DebugFlag.DEBUG;
    }

    // Use of this method should be limited to serious error cases that would cause the Agent to
    // shut down if not caught.
    private static final void logIfNRDebug(String msg, Throwable th) {
        if (isNewRelicDebug()) {
            System.out.println("While bootstrapping the Agent: " + msg + ": " + th.getStackTrace());
        }
    }

}
