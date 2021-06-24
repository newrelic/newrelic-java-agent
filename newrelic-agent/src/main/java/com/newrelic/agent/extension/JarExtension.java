/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.bootstrap.BootstrapAgent;
import com.newrelic.bootstrap.BootstrapLoader;
import com.newrelic.weave.utils.Streams;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * A jar extension represents a jar file which either contains several xml or yml extension files, or a set of weave
 * class files.
 */
public class JarExtension {

    private final ClassLoader classloader;
    private final File file;

    private final Map<String, Extension> extensions = new HashMap<>();

    /**
     * Factory to create a jar extension that loads an extension from a custom extension jar.
     *
     * If the manifest of this jar specifies an agent premain class, its premain method will be invoked. Any classes in
     * the jar that reference dependencies that the agent repackages will be rewritten to use the agent's package.
     *
     * @see #getAgentClass(Manifest)
     */
    public static JarExtension create(IAgentLogger logger, ExtensionParsers extensionParsers, File file)
            throws IOException {

        String agentClass;
        JarFile jar = new JarFile(file);
        ClassLoader jarClassLoader = new URLClassLoader(new URL[] { file.toURI().toURL() }, AgentBridge.getAgent().getClass().getClassLoader());

        try {
            agentClass = getAgentClass(jar.getManifest());
            if (null != agentClass) {
                logger.log(Level.FINE, "Detected agentmain class {0} in {1}", agentClass, file.getAbsolutePath());
                if (BootstrapAgent.class.getName().equals(agentClass)) {
                    throw new IOException("Attempt to load the New Relic agent from the extensions directory.  Remove " +
                            file.getName() + " from directory " + file.getParent());
                }

                // Rewrite jar references to dependency classes we repackage to use our repackaged name.
                byte[] newBytes = ExtensionRewriter.rewrite(jar, jarClassLoader);
                if (null != newBytes) {
                    file = writeTempJar(logger, file, newBytes);
                }
            }
        } finally {
            jar.close();
        }

        JarExtension ext = new JarExtension(logger, extensionParsers, file, jarClassLoader, true);

        if (agentClass != null) {
            ext.invokeMainMethod(logger, agentClass);
        }

        return ext;
    }

    /**
     * Factory to create a jar extension that loads an extension from the agent jar.
     */
    public static JarExtension create(IAgentLogger logger, ExtensionParsers extensionParsers, String jarFileName)
            throws IOException {
        return new JarExtension(logger, extensionParsers, new File(jarFileName), AgentBridge.getAgent().getClass().getClassLoader(),
                false);
    }

    private JarExtension(IAgentLogger logger, ExtensionParsers extensionParsers, File file, ClassLoader classLoader,
            boolean custom) throws IOException {
        this.classloader = classLoader;
        this.file = file;
        JarFile jarFile = new JarFile(file);

        logger.fine(MessageFormat.format(!custom ? "Loading built-in agent extensions"
                : "Loading extension jar \"{0}\"", file.getAbsolutePath()));

        Collection<JarEntry> entries = getExtensions(jarFile);
        for (JarEntry entry : entries) {
            try (InputStream iStream = jarFile.getInputStream(entry)) {
                if (iStream != null) {
                    try {
                        Extension extension = extensionParsers.getParser(entry.getName()).parse(classLoader, iStream,
                                custom);
                        addExtension(extension);
                    } catch (Exception ex) {
                        logger.severe(MessageFormat.format("Invalid extension file {0} : {1}", entry.getName(),
                                ex.toString()));
                        logger.log(Level.FINER, ex.toString(), ex);

                    }
                } else {
                    logger.fine(MessageFormat.format("Unable to load extension resource \"{0}\"", entry.getName()));
                }
            }
        }
    }

    public ClassLoader getClassloader() {
        return classloader;
    }

    /**
     * Returns a map of extensions. The key is the extension name.
     *
     */
    public final Map<String, Extension> getExtensions() {
        return Collections.unmodifiableMap(extensions);
    }

    void addExtension(Extension extension) {
        Extension existing = extensions.get(extension.getName());
        if (existing == null || existing.getVersionNumber() < extension.getVersionNumber()) {
            extensions.put(extension.getName(), extension);
        }
    }

    private static Collection<JarEntry> getExtensions(JarFile file) {
        List<JarEntry> list = readExtensionsFromManifest(file);

        // If the fast path (manifest) failed, fall back to iterating over contents of jar (slow)
        if (list.isEmpty()) {
            Pattern pattern = Pattern.compile("^META-INF/extensions/(.*).(yml|xml)$");
            for (Enumeration<JarEntry> entries = file.entries(); entries.hasMoreElements(); ) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (pattern.matcher(name).matches()) {
                    list.add(entry);
                }
            }
        }

        return list;
    }

    /**
     * This method reads the extensions manifest from the jar in order to quickly
     * identify and load the built-in extensions. This is an optimization over the
     * existing approach which simply iterated over the full contents of the jar
     * to find the correct files, which is slow and continues to get slower as we
     * add more files to the jar.
     *
     * @param file the agent jar file to load the extensions manifest from
     * @return a list of jar entries that map to built-in extensions
     */
    private static List<JarEntry> readExtensionsFromManifest(JarFile file) {
        List<JarEntry> list = new ArrayList<>();

        JarEntry extensionsManifestEntry = file.getJarEntry("META-INF/extensions/extensions");
        if (extensionsManifestEntry != null) {
            try (InputStream extensionsManifestStream = file.getInputStream(extensionsManifestEntry)) {
                if (extensionsManifestStream != null) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(extensionsManifestStream, StandardCharsets.UTF_8));
                    String filename = null;
                    while ((filename = in.readLine()) != null) {
                        if (!filename.trim().isEmpty()) {
                            JarEntry jarEntry = file.getJarEntry("META-INF/extensions/" + filename);
                            if (jarEntry == null) {
                                Agent.LOG.log(Level.FINE, "Error reading {0} from {1}", filename, file.getName());
                            } else {
                                list.add(jarEntry);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Agent.LOG.log(Level.FINE, "Error reading extensions from manifest", e);
            }
        }

        return list;
    }

    /**
     * Returns true if this extension contains any class marked with a {@link Weave} annotation.
     *
     */
    public boolean isWeaveInstrumentation() {
        return isWeaveInstrumentation(file);
    }

    /**
     * Returns true if this extension contains any class marked with a {@link Weave} annotation.
     *
     */
    public static boolean isWeaveInstrumentation(File file) {

        Collection<String> classNames = getClassFileNames(file);

        if (!classNames.isEmpty()) {
            if (!file.exists()) {
                return false;
            }

            try (JarFile jarFile = new JarFile(file)) {
                for (String fileName : classNames) {
                    JarEntry jarEntry = jarFile.getJarEntry(fileName);
                    try (InputStream stream = jarFile.getInputStream(jarEntry)) {
                        if (stream != null) {
                            ClassReader reader = new ClassReader(stream);

                            if (WeaveUtils.isWeavedClass(reader)) {
                                return true;
                            }
                        }
                    } catch (IOException e) {
                        Agent.LOG.log(Level.INFO, "Error processing " + fileName, e);
                    }
                    // bah
                }
            } catch (IOException ex) {
                Agent.LOG.log(Level.INFO, "Error processing extension jar " + file, ex);
            }
        }
        return false;
    }

    public Collection<String> getClassFileNames() {
        return getClassFileNames(file);
    }

    public static Collection<String> getClassFileNames(File file) {
        if (file.exists()) {
            try (JarFile jarFile = new JarFile(file)) {

                Collection<String> classes = new ArrayList<>();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        String fileName = entry.getName();

                        try {
                            classes.add(fileName);
                        } catch (Exception ex) {
                            // acceptable
                        }
                    }
                }

                return classes;
            } catch (IOException e) {
                Agent.LOG.debug("Unable to read classes in " + file.getAbsolutePath() + ".  " + e.getMessage());
            }
        }
        return Collections.emptyList();
    }

    public Collection<Class<?>> getClasses() {
        Collection<String> classNames = getClassFileNames();
        if (classNames.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<Class<?>> classes = new ArrayList<>();
        for (String fileName : classNames) {
            int index = fileName.indexOf(".class");
            fileName = fileName.substring(0, index);
            fileName = fileName.replace('/', '.');

            try {
                classes.add(classloader.loadClass(fileName));
            } catch (Exception ex) {
                // acceptable
            }
        }
        return classes;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return file.getAbsolutePath();
    }

    /**
     * Return the Agent-Class or Premain-Class attribute value if either exists.
     *
     * @param manifest
     */
    private static String getAgentClass(Manifest manifest) {
        for (String attr : Arrays.asList("Agent-Class", "Premain-Class")) {
            String agentClass = manifest.getMainAttributes().getValue(attr);
            if (null != agentClass) {
                return agentClass;
            }
        }
        return null;
    }

    /**
     * Invoke the premain method for the given class.
     *
     * @see Instrumentation
     */
    private void invokeMainMethod(IAgentLogger logger, String agentClass) {
        try {
            Class<?> clazz = classloader.loadClass(agentClass);
            logger.log(Level.FINE, "Invoking {0}.premain method", agentClass);
            Method method = clazz.getDeclaredMethod("premain", String.class, Instrumentation.class);
            String agentArgs = "";
            method.invoke(null, agentArgs, ServiceFactory.getClassTransformerService().getExtensionInstrumentation());
        } catch (ClassNotFoundException | SecurityException e) {
            logger.log(Level.INFO, "Unable to load {0}", agentClass);
            logger.log(Level.FINEST, e, e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.log(Level.INFO, "{0} has no premain method", agentClass);
            logger.log(Level.FINEST, e, e.getMessage());
        } catch (Exception e) {
            logger.log(Level.INFO, "Unable to invoke {0}.premain", agentClass);
            logger.log(Level.FINEST, e, e.getMessage());
        }
    }

    private static File writeTempJar(IAgentLogger logger, File file, byte[] newBytes) throws IOException {
        File original = file;
        file = File.createTempFile(file.getName(), ".jar", BootstrapLoader.getTempDir());
        file.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(file)) {
            Streams.copy(new ByteArrayInputStream(newBytes), out, newBytes.length);
        }
        logger.log(Level.FINER, "Rewriting {0} as {1}", original.getAbsolutePath(), file.getAbsolutePath());

        return file;
    }

}
