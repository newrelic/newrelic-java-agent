/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import java.io.IOException;
import java.lang.NumberFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.objectweb.asm.tree.ClassNode;

/**
 * Holds metadata and config options for a {@link WeavePackage}. Use {@link #builder()} to instantiate.
 */
public class WeavePackageConfig {
    /**
     * Creates a WeavePackageConfig using the builder pattern.
     */
    public static class Builder {
        private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.?\\d*)(.*)");

        private String name = null;
        private Instrumentation instrumentation = null;
        private String alias = null;
        private String vendorId = null;
        private float version = 1.0f;
        private String source = null;
        private boolean enabled = true;
        private boolean custom = false;
        private ClassNode errorHandleClassNode = ErrorTrapHandler.NO_ERROR_TRAP_HANDLER;
        private WeavePreprocessor preprocessor = WeavePreprocessor.NO_PREPROCESSOR;
        private WeavePostprocessor postprocessor = WeavePostprocessor.NO_POSTPROCESSOR;
        private ClassNode extensionClassTemplate = ExtensionClassTemplate.DEFAULT_EXTENSION_TEMPLATE;

        /**
         * Set the name of the weave package. This is the only required parameter.
         *
         * @param name package name; has no effect if null
         * @return builder with updated state
         */
        public Builder name(String name) {
            if (null != name) {
                this.name = name;
            }
            return this;
        }

        /**
         * Set the instance of {@link Instrumentation} to be used for weaving the bootstrap.
         * 
         * @param instrumentation {@link Instrumentation} to be used for weaving the bootstrap; has no effect if null
         * @return builder with updated state
         */
        public Builder instrumentation(Instrumentation instrumentation) {
            if (null != instrumentation) {
                this.instrumentation = instrumentation;
            }
            return this;
        }

        /**
         * Set an optional alias (for backwards compatibility with pointcuts) to be used for enabling/disabling the package.
         * 
         * @param alias optional alias; has no effect if null
         * @return builder with updated state
         */
        public Builder alias(String alias) {
            if (null != alias) {
                this.alias = alias;
            }
            return this;
        }

        /**
         * Set an optional vendor Id to make it easier to find the owner of an external weave module. e.g. - FIT
         *
         * @param vendorId optional vendorId; has no effect if null
         * @return builder with updated state
         */
        public Builder vendorId(String vendorId) {
            if (null != vendorId) {
                this.vendorId = vendorId;
            }
            return this;
        }

        /**
         * Set the of the weave package.
         * 
         * @param version the version of the weave package
         * @return builder with updated state
         */
        public Builder version(float version) {
            this.version = version;
            return this;
        }

        /**
         * Set the source the {@link WeavePackage} came from, usually a URI to a jar.
         * 
         * @param source where the {@link WeavePackage} came from, usually a URI to a jar; has no effect if null
         * @return builder with updated state
         */
        public Builder source(String source) {
            if (null != source) {
                this.source = source;
            }
            return this;
        }

        /**
         * Set whether or not the package is enabled
         * 
         * @param enabled whether or not the package is enabled
         * @return builder with updated state
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Set whether or not the package is "custom".
         *
         * @param custom whether or not the package is "custom"
         * @return builder with updated state
         */
        public Builder custom(boolean custom) {
            this.custom = custom;
            return this;
        }

        /**
         * Set the {@link ClassNode} conforming to the {@link ErrorTrapHandler} specification to be used as the error
         * trap.
         * 
         * @param errorHandleClassNode error trap; has no effect if null
         * @return builder with updated state
         */
        public Builder errorHandleClassNode(ClassNode errorHandleClassNode) {
            if (null != errorHandleClassNode) {
                this.errorHandleClassNode = errorHandleClassNode;
            }
            return this;
        }

        /**
         * Set the {@link WeavePreprocessor} that will be used to pre-process all classes loaded from this package.
         * @param preprocessor weave pre-processor; has no effect if null
         * @return builder with updated state
         */
        public Builder weavePreprocessor(WeavePreprocessor preprocessor) {
            if (null != preprocessor) {
                this.preprocessor = preprocessor;
            }
            return this;
        }

        /**
         * Set the {@link WeavePostprocessor} that will be used to post-process all classes loaded from this package.
         * @param postprocessor weave post-processor; has no effect if null
         * @return builder with updated state
         */
        public Builder weavePostprocessor(WeavePostprocessor postprocessor) {
            if (null != postprocessor) {
                this.postprocessor = postprocessor;
            }
            return this;
        }

        /**
         * Set the {@link ClassNode} conforming to the {@link ExtensionClassTemplate} specification, to be used as the backing
         * store for new fields.
         * @param extensionClassTemplate extension class template; has no effect if null
         * @return builder with updated state
         */
        public Builder extensionClassTemplate(ClassNode extensionClassTemplate) {
            if (null != extensionClassTemplate) {
                if (ExtensionClassTemplate.isValidExtensionNode(extensionClassTemplate)) {
                    this.extensionClassTemplate = extensionClassTemplate;
                } else {
                    throw new RuntimeException("Invalid extensionClassNode!");
                }
            }
            return this;
        }

        /**
         * Use a jar's manifest to set builder parameters.
         *
         * @param jarURL URL to a jar file.
         * @return Builder with updated state.
         * @throws Exception If the jar's manifest is incorrect or absent, or if the jar cannot be read.
         */
        public Builder url(URL jarURL) throws Exception {
            try (JarInputStream jarStream = new JarInputStream(jarURL.openStream())) {
                return this.jarInputStream(jarStream).source(jarURL.toString());
            }
        }

        /**
         * Use a jarStream's manifest to set builder parameters.
         *
         * @param jarStream jar with the manifest to build from. Will not be closed by this method.
         * @return Builder with updated state.
         * @throws Exception If the jar's manifest is incorrect or absent, or if the jar cannot be read.
         */
        public Builder jarInputStream(JarInputStream jarStream) throws Exception {
            if (jarStream.getManifest() == null) {
                throw new IOException("The instrumentation jar did not contain a manifest");
            }
            Attributes mainAttributes = jarStream.getManifest().getMainAttributes();
            String name = mainAttributes.getValue("Implementation-Title");
            if (name == null) {
                throw new Exception("The Implementation-Title of an instrumentation package is undefined");
            }

            String alias = mainAttributes.getValue("Implementation-Title-Alias");

            // This attribute is optional and is intended to be used by FIT
            String vendorId = mainAttributes.getValue("Implementation-Vendor-Id");

            String implementationVersion = mainAttributes.getValue("Implementation-Version");
            float version = this.version;
            if (null != implementationVersion) {
                try {
                    final Matcher matcher = VERSION_PATTERN.matcher(implementationVersion);

                    if (matcher.matches()) {
                        version = Float.parseFloat(matcher.group(1));
                    }
                } catch (NumberFormatException ignore) {
                    // Ignore
                }
            }

            boolean enabled = this.enabled;
            String enabledS = mainAttributes.getValue("Enabled");
            if (null != enabledS) {
                enabled = Boolean.parseBoolean(enabledS);
            }

            return this.name(name).alias(alias).vendorId(vendorId).version(version).enabled(enabled);
        }

        /**
         * Build the WeavePackageConfig using the parameters that have been passed in so far.
         *
         * @return the build WeavePackageConfig.
         * @throws RuntimeException If the config's name has not been set.
         */
        public WeavePackageConfig build() {
            if (null == name) {
                throw new RuntimeException("WeavePackageConfig must have an Implementation-Name");
            }
            return new WeavePackageConfig(name, alias, vendorId, version, enabled, source, custom, instrumentation,
                    errorHandleClassNode, preprocessor, postprocessor, extensionClassTemplate);
        }
    }

    /**
     * Create a {@link Builder} for WeavePackageConfig.
     *
     * @return config builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // other potential options:
    // priority of matches (order to apply)
    // multi-match strategy (short-circuit, chain)
    // violation fail-fast

    private final Instrumentation instrumentation;
    private final String name;
    private final String alias;
    private final String vendorId;
    private final float version;
    private final String source;
    private final boolean enabled;
    private final boolean custom;
    private final ClassNode errorHandleClassNode;
    private final WeavePreprocessor preprocessor;
    private final WeavePostprocessor postprocessor;
    private final ClassNode extensionClassTemplate;

    private WeavePackageConfig(String name, String alias, String vendorId, float version, boolean enabled,
            String source, boolean custom, Instrumentation instrumentation, ClassNode errorTrapClassNode,
            WeavePreprocessor preprocessor, WeavePostprocessor postprocessor, ClassNode extensionClassTemplate) {
        this.name = name;
        this.alias = alias;
        this.vendorId = vendorId;
        this.version = version;
        this.enabled = enabled;
        this.source = source;
        this.custom = custom;

        this.instrumentation = instrumentation;
        this.errorHandleClassNode = errorTrapClassNode;
        this.preprocessor = preprocessor;
        this.postprocessor = postprocessor;
        this.extensionClassTemplate = extensionClassTemplate;
    }

    /**
     * The name of the {@link WeavePackage}.
     */
    public String getName() {
        return name;
    }

    /**
     * The optional alias of the {@link WeavePackage}. This will generally be set for backwards compatibility reasons
     * when a pointcut is moved to a weaved module.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * The optional vendorId of the {@link WeavePackage}. This will usually only be set for FIT modules and we use it
     * to indicate the module as such for now.
     */
    public String getVendorId() {
        return vendorId;
    }

    /**
     * The version of the {@link WeavePackage}.
     */
    public float getVersion() {
        return version;
    }

    /**
     * The source the {@link WeavePackage} came from, usually a URI to a jar.
     */
    public String getSource() {
        return source;
    }

    /**
     * Whether or not the {@link WeavePackage} is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether or not the {@link WeavePackage} is "custom", i.e. loaded by the agent from the /extensions directory.
     */
    public boolean isCustom() {
        return custom;
    }

    /**
     * The instance of {@link Instrumentation} to be used for managing the bootstrap, or <code>null</code> if not
     * available.
     */
    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * The {@link ClassNode} conforming to the {@link ErrorTrapHandler} specification, to be used as the error trap when
     * weaving all classes in this package.
     */
    public ClassNode getErrorHandleClassNode() {
        return this.errorHandleClassNode;
    }

    /**
     * The {@link WeavePreprocessor} that will be used to pre-process all classes loaded from this weave package.
     */
    public WeavePreprocessor getPreprocessor() {
        return this.preprocessor;
    }

    /**
     * The {@link WeavePostprocessor} that will be used to post-process all classes loaded from this weave package.
     */
    public WeavePostprocessor getPostprocessor() {
        return this.postprocessor;
    }

    /**
     * The {@link ClassNode} conforming to the {@link ExtensionClassTemplate} specification, to be used as the backing
     * store for new fields.
     */
    public ClassNode getExtensionTemplate() {
        return this.extensionClassTemplate;
    }

    @Override
    public String toString() {
        return "WeavePackageConfig [name=" + name + ", version=" + version + ", enabled=" + enabled + "]";
    }
}
