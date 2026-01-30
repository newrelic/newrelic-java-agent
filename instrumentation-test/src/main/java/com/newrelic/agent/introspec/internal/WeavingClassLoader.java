/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.agent.deps.org.objectweb.asm.ClassReader;
import com.newrelic.agent.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.agent.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.agent.deps.org.objectweb.asm.commons.Method;
import com.newrelic.agent.deps.org.objectweb.asm.Type;
import com.newrelic.agent.deps.org.objectweb.asm.tree.ClassNode;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.api.agent.weaver.scala.ScalaWeave;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.utils.WeaveClassInfo;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.ClassWeavedListener;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageConfig;
import com.newrelic.weave.weavepackage.WeavePackageManager;
import com.newrelic.weave.weavepackage.WeavePostprocessor;
import com.newrelic.weave.weavepackage.WeavePreprocessor;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

class WeavingClassLoader extends TransformingClassLoader {

    public static final String TEST_WEAVE_PACKAGE_NAME = "test-package";
    public static final String TEST_WEAVE_PACKAGE_SOURCE = "test-package-source";
    private static final ClassResource NO_RESOURCE = new ClassResource(null, null);
    private static final String NEWRELIC_API_CLASS = "com.newrelic.api.agent.NewRelic";
    private static final String AGENT_BRIDGE_CLASS = "com.newrelic.agent.bridge.AgentBridge";
    private static final String WEAVE_TEST_CONFIG_DESC = Type.getType(InstrumentationTestConfig.class).getDescriptor();
    private static final String SCALA_WEAVE_DESC = Type.getType(ScalaWeave.class).getDescriptor();

    protected final HashMap<String, ClassResource> shadowedClassResources = new HashMap<>();
    protected final WeavePackageManager weavePackageManager = new WeavePackageManager(new FailingWeavePackageListener());
    protected final ClassCache classCache = new ClassCache(new ClassLoaderFinder(this));
    protected final WeaveIncludes weaveIncludes;
    protected final ClassNode errorTrapClassNode;
    protected final WeavePreprocessor weavePreprocessor;
    protected final WeavePostprocessor weavePostprocessor;

    public WeavingClassLoader(URLClassLoader parent, WeaveIncludes weaveIncludes, ClassNode errorTrapClassNode,
            WeavePreprocessor weavePreprocessor, WeavePostprocessor weavePostprocessor) throws InitializationError {
        super(parent);
        this.weaveIncludes = weaveIncludes;
        this.errorTrapClassNode = errorTrapClassNode;
        this.weavePreprocessor = weavePreprocessor;
        this.weavePostprocessor = weavePostprocessor;

        try {
            processClassPath();
        } catch (IOException ioe) {
            throw new InitializationError(ioe);
        }
    }

    private void processClassPath() throws IOException {

        // scan for @Weave or @SkipIfPresent classes and the classes they shadow
        Set<String> weaveResourceNames = new HashSet<>();
        List<byte[]> weavePackageClasses = new ArrayList<>();
        Set<URL> weavePackageURLs = new HashSet<>();

        // also scan for NewRelic classes and AgentBridge class
        // we use this later to prevent the API JAR's NewRelic class from shadowing the bridge JAR's NewRelic class
        Set<ClassResource> newRelicApiClasses = new HashSet<>();
        URL agentBridgeSourceURL = null;

        List<ClassResource> classResources = ClassResource.fromClassLoader((URLClassLoader) getParent());
        for (ClassResource classResource : classResources) {

            String resourceName = classResource.resourceName;
            String className = resourceName.replace('/', '.').replace(".class", "");
            if (weaveIncludes.isIncluded(className)) {
                byte[] bytes = classResource.read();
                if (usesWeaver(bytes) || isInstrumentationUtilityClass(classResource)) {
                    weaveResourceNames.add(resourceName);
                    weavePackageClasses.add(bytes);
                    weavePackageURLs.add(classResource.sourceURL);
                } else {
                    // Everything else
                    shadowedClassResources.put(resourceName, classResource);
                }
            } else if (weaveIncludes.classUnderTestName.startsWith(className)) {
                weavePackageURLs.add(classResource.sourceURL);
            } else if (className.equals(NEWRELIC_API_CLASS)) {
                newRelicApiClasses.add(classResource);
            } else if (className.equals(AGENT_BRIDGE_CLASS)) {
                agentBridgeSourceURL = classResource.sourceURL;
            }
        }

        // remove resources that aren't actually shadowed
        shadowedClassResources.keySet().retainAll(weaveResourceNames);

        // make sure weave resources that don't have an original class don't return a class
        // this is necessary to support @SkipIfPresent
        for (String name : weaveResourceNames) {
            if (!shadowedClassResources.containsKey(name)) {
                shadowedClassResources.put(name, NO_RESOURCE);
            }
        }

        // create weave package for testing
        // @formatter:off
        WeavePackageConfig config = WeavePackageConfig.builder()
                .name(TEST_WEAVE_PACKAGE_NAME)
                .source(TEST_WEAVE_PACKAGE_SOURCE)
                .errorHandleClassNode(errorTrapClassNode)
                .weavePreprocessor(weavePreprocessor)
                .weavePostprocessor(weavePostprocessor)
                .build();
        // @formatter:on

        WeavePackage weavePackage = new WeavePackage(config, weavePackageClasses);
        weavePackageManager.register(weavePackage);

        // handle NewRelic API class shadowing
        if (agentBridgeSourceURL != null) {
            for (ClassResource newRelicApiClass : newRelicApiClasses) {
                if (agentBridgeSourceURL.equals(newRelicApiClass.sourceURL)) {
                    shadowedClassResources.put(newRelicApiClass.resourceName, newRelicApiClass);
                }
            }
        }
    }

    private boolean isInstrumentationUtilityClass(ClassResource classResource) {
        return classResource.sourceURL.toString().endsWith("/build/classes/main/") ||
                classResource.sourceURL.toString().endsWith("/build/classes/java/main/") ||
                classResource.sourceURL.toString().endsWith("/build/classes/scala/main/") ||
                classResource.sourceURL.toString().endsWith("/build/classes/kotlin/main/");
    }

    @Override
    protected boolean canTransform(String className) {
        return className.equals(NEWRELIC_API_CLASS) || super.canTransform(className);
    }

    public static boolean usesWeaver(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        final boolean[] foundWeaveAnnotation = { false };
        reader.accept(new ClassVisitor(WeaveUtils.ASM_API_LEVEL) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (desc.equals(WeaveClassInfo.WEAVE_DESC) || desc.equals(WeaveClassInfo.SKIP_IF_PRESENT_DESC)
                        || desc.equals(SCALA_WEAVE_DESC) || desc.equals(WeaveClassInfo.WEAVE_ANNOTATED_TYPE_DESC)) {
                    foundWeaveAnnotation[0] = true;
                }
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (desc.equals(WeaveClassInfo.WEAVE_ANNOTATED_TYPE_DESC)) {
                            foundWeaveAnnotation[0] = true;
                        }
                        return null;
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return foundWeaveAnnotation[0];
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        ClassResource shadowedResource = shadowedClassResources.get(WeaveUtils.getClassResourceName(name));
        if (shadowedResource != null) {
            if (shadowedResource == NO_RESOURCE) {
                return null;
            }

            try {
                byte[] bytes = shadowedResource.read();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return super.findClass(name);
        } catch (Throwable t) {
            // fallback
            return Class.forName(name);
        }
    }

    @Override
    public URL getResource(String name) {
        ClassResource shadowedResource = shadowedClassResources.get(name);
        if (shadowedResource != null) {
            if (shadowedResource == NO_RESOURCE) {
                return null;
            }

            try {
                return shadowedResource.getResourceURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return super.getResource(name);
    }

    protected byte[] weave(String className,
                           byte[] classBytes,
                           Map<Method, Collection<String>> skipMethods,
                           ClassWeavedListener listener) throws IOException {
        return weavePackageManager.weave(this, classCache, className.replace('.', '/'), classBytes, skipMethods,
                                         listener);
    }

    @Override
    protected byte[] transform(String className) throws Exception {
        byte[] classBytes = WeaveUtils.getClassBytesFromClassLoaderResource(className, this);
        return classBytes == null ? null : weave(className, classBytes, Collections.emptyMap(), null);
    }
}
