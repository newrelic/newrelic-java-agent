/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.config.ConfigFileHelper;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.extension.ExtensionParsers.ExtensionParser;
import com.newrelic.agent.extension.util.ExtensionConversionUtility;
import com.newrelic.agent.instrumentation.context.ClassesMatcher;
import com.newrelic.agent.instrumentation.context.InstrumentationContextClassMatcherHelper;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.reinstrument.ReinstrumentResult;
import com.newrelic.agent.reinstrument.ReinstrumentUtils;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.Service;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Extensions are jars, xml, or yml configuration files that allow the agent to be extended with extra instrumentation
 * and additional JMX configuration. Multiple extension files can be combined into a single jar extension. The agent
 * reads extensions from within the agent jar file itself by loading all of the xml and yml files in
 * META-INF/extensions. It also reads from the extensions directory in the New Relic home directory.
 *
 * Extensions directory is checked every harvest for changes. If changes are detected, the instrumentation is reloaded
 * and matching classes are retransformed.
 */
public class ExtensionService extends AbstractService implements HarvestListener {
    private final ConfigService config;
    private final ExtensionsLoadedListener extensionsLoadedListener;
    private ExtensionParsers extensionParsers;

    /**
     * This contains the agent internal yml and xml extensions.
     */
    private final Map<String, Extension> internalExtensions = new HashMap<>();
    private volatile Set<Extension> extensions = Collections.emptySet();
    private final List<ExtensionClassAndMethodMatcher> pointCuts = new ArrayList<>();

    /**
     * A map of weave files to their timestamp.
     */
    private final Map<File, Long> weaveExtensions = new HashMap<>();

    private final List<Service> services = new ArrayList<>();
    private final List<ConfigurationConstruct> constructs = new ArrayList<>();

    private long lastReloaded = 0;
    private int elementCount = -1;

    public ExtensionService(ConfigService configService, ExtensionsLoadedListener extensionsLoadedListener) {
        super(ExtensionService.class.getSimpleName());
        config = configService;
        this.extensionsLoadedListener = extensionsLoadedListener;
    }

    /**
     * Always enabled to support dynamic instrumentation.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected void doStart() {
        if (isEnabled()) {
            extensionParsers = new ExtensionParsers(constructs);

            try {
                initializeBuiltInExtensions();
                loadExtensionJars();
                reloadCustomExtensionsIfModified();
                reloadWeaveInstrumentationIfModified();
            } catch (NoSuchMethodError e) {
                // smd: I was seeing an error through this path on tomcat 5 because of the xml libraries in
                // common/endorsed
                Agent.LOG.error("Unable to initialize agent extensions.  The likely cause is duplicate copies of javax.xml libraries.");
                Agent.LOG.log(Level.FINE, e.toString(), e);
            } catch (NoClassDefFoundError e) {
                Agent.LOG.error("Unable to initialize agent extensions. The likely cause is an incorrectly configured javax.xml.");
                Agent.LOG.log(Level.FINE, e, "");
            }

            extensionsLoadedListener.loaded(getWeaveExtensions());
        }
    }

    @Override
    protected void doStop() {
        internalExtensions.clear();
        pointCuts.clear();
        weaveExtensions.clear();
        for (Service service : services) {
            try {
                service.stop();
            } catch (Exception e) {
                String msg = MessageFormat.format("Unable to stop extension service \"{0}\" - {1}", service.getName(),
                        e.toString());
                Agent.LOG.severe(msg);
                getLogger().log(Level.FINE, msg, e);
            }
        }
        services.clear();
    }

    @Override
    public void beforeHarvest(String pAppName, StatsEngine pStatsEngine) {
        // no-op
    }

    @Override
    public void afterHarvest(String pAppName) {
        if (!config.getDefaultAgentConfig().getApplicationName().equals(pAppName)) {
            return;
        }

        if (ServiceFactory.getCoreService().getInstrumentation().isRetransformClassesSupported() &&
                config.getDefaultAgentConfig().getExtensionsConfig().shouldReloadModified()) {
            reloadCustomExtensionsIfModified();
            reloadWeaveInstrumentationIfModified();
        } else {
            Agent.LOG.log(Level.FINEST, "Retransformation is not supported - not reloading extensions.");
        }
    }

    // This should only be used for testing!!
    protected void addInternalExtensionForTesting(Extension ext) {
        internalExtensions.put(ext.getName(), ext);
    }

    private void initializeBuiltInExtensions() {
        ClassTransformerConfig classTransformerConfig = config.getDefaultAgentConfig().getClassTransformerConfig();

        String jarFileName = AgentJarHelper.getAgentJarFileName();
        if (jarFileName == null) {
            getLogger().log(Level.SEVERE, "Unable to find the agent jar file");
            return;
        }

        boolean defaultInstrumentationDisabled = !classTransformerConfig.isDefaultInstrumentationEnabled();
        boolean builtinExtensionsDisabled = !classTransformerConfig.isBuiltinExtensionEnabled();
        boolean builtinExtensionsExplicitlyEnabled = classTransformerConfig.isBuiltinExtensionEnabled();

        if (defaultInstrumentationDisabled && !builtinExtensionsExplicitlyEnabled) {
            getLogger().log(Level.FINEST, "Instrumentation is disabled by default. Not loading builtin extensions");
        } else if (builtinExtensionsDisabled) {
            getLogger().log(Level.INFO, "Builtin extensions are disabled");
        } else {
            try {
                JarExtension jarExtension = JarExtension.create(getLogger(), extensionParsers, jarFileName);
                addJarExtensions(jarExtension);
            } catch (IOException e) {
                getLogger().severe(MessageFormat.format("Unable to read extensions from the agent jar : {0}", e.toString()));
                getLogger().log(Level.FINER, "Extensions error", e);
            }
        }
    }

    private void loadExtensionJars() {
        Collection<JarExtension> jarExtensions = loadJarExtensions(getExtensionDirectory());
        for (JarExtension extension : jarExtensions) {
            if (extension.isWeaveInstrumentation()) {
                // Skip. Weave instrumentation is added through a different path.
            } else {
                try {
                    for (Class<?> clazz : extension.getClasses()) {
                        noticeExtensionClass(clazz);
                    }
                    addJarExtensions(extension);
                } catch (Throwable t) {
                    Agent.LOG.log(Level.INFO, "An error occurred adding extension {0} : {1}", extension.getFile(),
                            t.getMessage());
                    Agent.LOG.log(Level.FINEST, t, t.getMessage());
                }
            }
        }
    }

    private void addJarExtensions(JarExtension jarExtension) {
        for (Extension extension : jarExtension.getExtensions().values()) {
            Extension validateExtension = validateExtension(extension, internalExtensions);
            if (validateExtension != null) {
                internalExtensions.put(extension.getName(), extension);
            }
        }
    }

    private void reloadCustomExtensionsIfModified() {
        File[] xmlFiles = getExtensionFiles(ExtensionFileTypes.XML.getFilter());
        File[] ymlFiles = getExtensionFiles(ExtensionFileTypes.YML.getFilter());

        // element count start at -1 to ensure fileModified is true the first time
        boolean fileModified = (xmlFiles.length + ymlFiles.length) != elementCount;
        if (!fileModified) {
            for (File file : xmlFiles) {
                fileModified |= (file.lastModified() <= System.currentTimeMillis() && lastReloaded < file.lastModified());
            }
            for (File file : ymlFiles) {
                fileModified |= (file.lastModified() <= System.currentTimeMillis() && lastReloaded < file.lastModified());
            }
        }

        // we always need to load the first time to get in the xml/yaml files within the agent
        // if you are changing be sure to test without an extensions directory
        if (fileModified) {
            lastReloaded = System.currentTimeMillis();
            elementCount = xmlFiles.length + ymlFiles.length;

            pointCuts.clear();
            HashMap<String, Extension> allExtensions = new HashMap<>(internalExtensions);
            loadValidExtensions(xmlFiles, extensionParsers.getXmlParser(), allExtensions);
            loadValidExtensions(ymlFiles, extensionParsers.getYamlParser(), allExtensions);
            Set<Extension> externalExtensions = new HashSet<>(allExtensions.values());
            externalExtensions.removeAll(internalExtensions.values());
            Set<Extension> oldExtensions = extensions;
            extensions = Collections.unmodifiableSet(externalExtensions);
            JmxService jmxService = ServiceFactory.getJmxService();
            if (jmxService != null) {
                jmxService.reloadExtensions(oldExtensions, extensions);
            }
            for (Extension extension : allExtensions.values()) {
                pointCuts.addAll(extension.getInstrumentationMatchers());
            }
            ClassRetransformer retransformer = ServiceFactory.getClassTransformerService().getLocalRetransformer();
            if (retransformer != null) {
                Class<?>[] allLoadedClasses = ServiceFactory.getCoreService().getInstrumentation().getAllLoadedClasses();
                retransformer.setClassMethodMatchers(pointCuts);
                InstrumentationContextClassMatcherHelper matcherHelper = new InstrumentationContextClassMatcherHelper();
                Set<Class<?>> classesToRetransform = ClassesMatcher.getMatchingClasses(
                        retransformer.getMatchers(), matcherHelper, allLoadedClasses);
                ReinstrumentUtils.checkClassExistsAndRetransformClasses(new ReinstrumentResult(),
                        Collections.<ExtensionClassAndMethodMatcher>emptyList(), null, classesToRetransform);
            }
        }
    }

    private void reloadWeaveInstrumentationIfModified() {
        File[] jarFiles = getExtensionFiles(ExtensionFileTypes.JAR.getFilter());
        Collection<File> weaveFiles = Collections2.filter(Arrays.asList(jarFiles), JarExtension::isWeaveInstrumentation);

        Collection<File> newWeaveFiles = new HashSet<>();
        Collection<File> removedWeaveFiles = new HashSet<>();

        for (File file : weaveFiles) {
            Long timestamp = weaveExtensions.get(file);
            if (timestamp == null || (timestamp <= System.currentTimeMillis() && timestamp != file.lastModified())) {
                newWeaveFiles.add(file);
            }
        }
        for (File file : weaveExtensions.keySet()) {
            if (!weaveFiles.contains(file)) {
                removedWeaveFiles.add(file);
            }
        }

        if (newWeaveFiles.size() > 0 || removedWeaveFiles.size() > 0) {
            weaveExtensions.clear();
            for (File file : weaveFiles) {
                weaveExtensions.put(file, file.lastModified());
            }

            InstrumentationContextManager contextManager = ServiceFactory.getClassTransformerService().getContextManager();
            if (contextManager != null) {
                contextManager.getClassWeaverService().reloadExternalWeavePackages(newWeaveFiles, removedWeaveFiles).run();
            }

            Agent.LOG.finer("Weave extension jars: " + weaveExtensions);
        }
    }

    /**
     * Reads in files using the input filter.
     *
     * @param filter Filter used to read in files.
     * @return The files matching the filter.
     */
    private File[] getExtensionFiles(final FileFilter filter) {
        File directory = getExtensionDirectory();
        if (directory == null) {
            return new File[] {};
        } else {
            return directory.listFiles(filter);
        }
    }

    /**
     * Retrieves the extension directory using the config property or the default directory.
     *
     * @return The config directory as a file or null if the directory does not exist or is not readable.
     */
    private File getExtensionDirectory() {
        AgentConfig agentConfig = config.getDefaultAgentConfig();
        String configDirName = agentConfig.getProperty(AgentConfigImpl.EXT_CONFIG_DIR);
        if (configDirName == null) {
            configDirName = ConfigFileHelper.getNewRelicDirectory() + File.separator
                    + ExtensionConversionUtility.DEFAULT_CONFIG_DIRECTORY;
        }
        File configDir = new File(configDirName);
        if (!configDir.exists()) {
            Agent.LOG.log(Level.FINE, "The extension directory " + configDir.getAbsolutePath() + " does not exist.");
            configDir = null;
        } else if (!configDir.isDirectory()) {
            Agent.LOG.log(Level.WARNING, "The extension directory " + configDir.getAbsolutePath()
                    + " is not a directory.");
            configDir = null;
        } else if (!configDir.canRead()) {
            Agent.LOG.log(Level.WARNING, "The extension directory " + configDir.getAbsolutePath() + " is not readable.");
            configDir = null;
        }
        return configDir;
    }

    /**
     * Gets the valid extension files and adds them to the extensions map.
     *
     * @param files Files to attempt to load as extensions
     * @param parser The parser that applies to all of this type of file extension
     * @param extensions - read extensions checked against this map and added if valid
     */
    private void loadValidExtensions(final File[] files, ExtensionParser parser, HashMap<String, Extension> extensions) {
        if (files != null) {
            for (File file : files) {
                getLogger().log(Level.FINER,
                        MessageFormat.format("Reading custom extension file {0}", file.getAbsolutePath()));
                try {
                    Extension currentExt = readExtension(parser, file);
                    currentExt = validateExtension(currentExt, extensions);
                    if (currentExt != null) {
                        extensions.put(currentExt.getName(), currentExt);
                    } else {
                        getLogger().log(Level.WARNING,
                                "Extension in file " + file.getAbsolutePath() + " could not be read in.");
                    }
                } catch (Exception ex) {
                    getLogger().severe("Unable to parse extension. Check permissions on " + file.getAbsolutePath() + ".  " + ex.toString());
                    getLogger().log(Level.FINE, ex.toString(), ex);
                }
            }
        }
    }

    private Extension readExtension(ExtensionParser parser, File file) throws Exception {
        try (FileInputStream iStream = new FileInputStream(file)) {
            return parser.parse(AgentBridge.getAgent().getClass().getClassLoader(), iStream, true);
        }
    }

    /**
     * Validates extension against existingExtensions for name and version issues.
     *
     * @param extension The extension to validate.
     * @param existingExtensions Map to compare against for version issues.
     * @return validated extension, or null if not valid.
     */
    protected Extension validateExtension(final Extension extension, Map<String, Extension> existingExtensions) {
        String name = extension.getName();
        if ((name != null) && (name.length() != 0)) {
            double version = extension.getVersionNumber();

            Extension existing = existingExtensions.get(name);
            if (existing == null) {
                getLogger().log(
                        Level.FINER,
                        MessageFormat.format("Adding extension with name {0} and version {1}", name, Double.valueOf(
                                version).toString()));
                return extension;
            } else if (version > existing.getVersionNumber()) {
                // use the new one instead of the old one
                getLogger().log(
                        Level.FINER,
                        MessageFormat.format("Updating extension with name {0} to version {1}", name, Double.valueOf(
                                version).toString()));
                return extension;
            } else {
                getLogger().log(
                        Level.FINER,
                        MessageFormat.format(
                                "Additional extension with name {0} and version {1} being ignored. Another file with name and version already read in.",
                                name, Double.valueOf(version).toString()));
            }
        }
        return null;
    }

    private void noticeExtensionClass(Class<?> clazz) {
        getLogger().finest(MessageFormat.format("Noticed extension class {0}", clazz.getName()));
        if (Service.class.isAssignableFrom(clazz)) {
            try {
                addService((Service) clazz.getConstructor().newInstance());
            } catch (Exception ex) {
                getLogger().severe(
                        MessageFormat.format("Unable to instantiate extension service \"{0}\"", clazz.getName()));
                getLogger().log(Level.FINE, "Unable to instantiate service", ex);
            }
        }
    }

    private void addService(Service service) {
        String msg = MessageFormat.format("Noticed extension service \"{0}\"", service.getName());
        getLogger().finest(msg);
        if (!service.isEnabled()) {
            return;
        }
        services.add(service);
        msg = MessageFormat.format("Starting extension service \"{0}\"", service.getName());
        getLogger().finest(msg);
        try {
            service.start();
        } catch (Exception e) {
            msg = MessageFormat.format("Unable to start extension service \"{0}\" - {1}", service.getName(),
                    e.toString());
            getLogger().severe(msg);
            getLogger().log(Level.FINE, msg, e);
        }
    }

    private Collection<JarExtension> loadJarExtensions(File jarDirectory) {
        if (jarDirectory == null || !jarDirectory.exists()) {
            return Collections.emptyList();
        }
        if (jarDirectory.isDirectory()) {
            return loadJars(jarDirectory.listFiles(ExtensionFileTypes.JAR.getFilter()));
        } else if (jarDirectory.exists()) {
            return loadJars(new File[] { jarDirectory });
        }
        return Collections.emptyList();
    }

    private Collection<JarExtension> loadJars(File[] jarFiles) {
        Collection<JarExtension> extensions = new ArrayList<>();
        for (File file : jarFiles) {
            try {
                JarExtension ext = JarExtension.create(getLogger(), extensionParsers, file);
                extensions.add(ext);
            } catch (Throwable ex) {
                Agent.LOG.severe("Unable to load extension " + file.getName());
                Agent.LOG.log(Level.FINER, ex.toString(), ex);
            }
        }
        return Collections.unmodifiableCollection(extensions);
    }

    /**
     * Returns the point cuts read in from extension files. This includes XML and YML files.
     *
     * @return The pointcuts form extension files.
     */
    public final List<ExtensionClassAndMethodMatcher> getEnabledPointCuts() {
        return pointCuts;
    }

    /**
     * This method allows other services to extend the yml parser to support their own custom yml.
     *
     * @param construct
     */
    public void addConstruct(ConfigurationConstruct construct) {
        constructs.add(construct);
    }

    /**
     * Returns a map of extensions. The key is the extension name.
     *
     * @return The map of xml and yml extensions.
     */
    public final Map<String, Extension> getInternalExtensions() {
        return Collections.unmodifiableMap(internalExtensions);
    }

    /**
     * Returns the extensions last loaded from the extensions directory. May change on harvest cycle if contents from
     * disk change.
     */
    public final Set<Extension> getExtensions() {
        return extensions;
    }

    /**
     * Returns the files that contain weave instrumentation.
     *
     */
    public Set<File> getWeaveExtensions() {
        return weaveExtensions.keySet();
    }

    @VisibleForTesting
    long getLastReloaded() {
        return lastReloaded;
    }
}
