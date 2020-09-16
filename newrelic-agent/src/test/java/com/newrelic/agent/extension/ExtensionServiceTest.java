/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.ExtensionsConfigImpl;
import com.newrelic.agent.instrumentation.ClassTransformerService;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.tracing.ParameterAttributeName;
import com.newrelic.agent.instrumentation.weaver.ClassWeaverService;
import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.weave.utils.Streams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class ExtensionServiceTest {

    private static final String EXTENSION_NAME = "test";

    /**
     * An xml file.
     */
    private static final String XML_FILE_PATH_1 = "xmlextension1.xml";
    /**
     * An xml file.
     */
    private static final String XML_FILE_PATH_1_1 = "xmlextension1_1.xml";

    /**
     * YML file with the same name of the xmlextension1_1.xml file.
     */
    private static final String YML_FILE_PATH_1_1 = "custom_extension_test.yml";

    /**
     * File that does not have a name or version.
     */
    private static final String XML_NO_NAME_OR_VERSION = "xmlNoAttributes.xml";

    /**
     * File that contains an invalid format.
     */
    private static final String XML_INVALID_FORMAT = "xmlInvalidFormat.xml";

    /**
     * A point cut with no method set.
     */
    private static final String XML_NO_METHOD = "xmlNoMethod.xml";

    /**
     * A file that contains methods with duplicates in it.
     */
    private static final String XML_DUPS = "xmlduplicates.xml";

    /**
     * A file that contains a methods with two parameters to capture.
     */
    private static final String XML_CAPTURE_METHOD_PARAMS = "xml-capture-attributes.xml";

    /**
     * A jar with valid weave instrumentation
     */
    public static final String WEAVE_INSTRUMENTATION = "spring-jms-2-1.0.jar";
    public static final String WEAVE_INSTRUMENTATION_2 = "jms-1.1-1.0.jar"; // Has new util classes

    /**
     * A mock service manager.
     */
    private MockServiceManager serviceManager;
    private ExtensionService extensionService;
    private ConfigService configService;

    @Before
    public void setUpAgent() throws Exception {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(AgentConfigImpl.APP_NAME, EXTENSION_NAME);
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);

        configService = ConfigServiceFactory.createConfigService(config, Collections.<String, Object>emptyMap());
        serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        final InstrumentationProxy instrumentationProxy = Mockito.mock(InstrumentationProxy.class);
        serviceManager.setCoreService(new MockCoreService() {
            @Override
            public InstrumentationProxy getInstrumentation() {
                return instrumentationProxy;
            }
        });
        Mockito.when(instrumentationProxy.isRetransformClassesSupported()).thenReturn(true);
        Mockito.when(instrumentationProxy.getAllLoadedClasses()).thenReturn(new Class[] {});

        extensionService = new ExtensionService(configService, ExtensionsLoadedListener.NOOP);
        serviceManager.setExtensionService(extensionService);
        serviceManager.setJmxService(Mockito.mock(JmxService.class));

        ClassTransformerService classTransformerService = serviceManager.getClassTransformerService();

        ClassRetransformer mockRetransformer = Mockito.mock(ClassRetransformer.class);
        Mockito.when(classTransformerService.getLocalRetransformer()).thenReturn(mockRetransformer);

        InstrumentationContextManager mockContextManager = Mockito.mock(InstrumentationContextManager.class);
        Mockito.when(classTransformerService.getContextManager()).thenReturn(mockContextManager);

        ClassWeaverService mockWeaverService = Mockito.mock(ClassWeaverService.class);
        Mockito.when(mockContextManager.getClassWeaverService()).thenReturn(mockWeaverService);

        Mockito.when(mockWeaverService.reloadExternalWeavePackages(Mockito.<File>anyCollection(), Mockito.<File>anyCollection())).thenReturn(
                new Runnable() {
                    @Override
                    public void run() {
                    }
                });
    }

    /**
     * Cleans up the extensions directory.
     */
    @After
    public void cleanUp() {
        if (System.getProperty("newrelic.config.extensions.dir") != null) {
            File dir = new File(System.getProperty("newrelic.config.extensions.dir"));
            if (dir.exists()) {
                if (dir.isDirectory()) {
                    File[] exts = dir.listFiles();
                    for (File ext : exts) {
                        ext.delete();
                    }
                }
                dir.delete();
            }
        }
        System.clearProperty("newrelic.config.extensions.dir");
        Mockito.validateMockitoUsage();
    }

    @Test
    public void testLoadInternalExtensionsLoadedIfNoExtensionDir() throws Exception {
        // prop cleared in the cleanup
        System.setProperty("newrelic.config.extensions.dir", "/my/directory/do/not/exist");
        Map<String, Object> props = new HashMap<>();
        props.put("version", 1.0);
        props.put("name", "jasper");
        Map<String, Object> inst = new HashMap<>();
        props.put("instrumentation", inst);
        List values = new ArrayList();
        inst.put("pointcuts", values);
        Map<String, Object> pc = new HashMap<>();
        values.add(pc);
        pc.put("class_matcher", "org/apache/jasper/compiler/Generator$GenerateVisitor");
        pc.put("method_matcher", "visit(Lorg/apache/jasper/compiler/Node$TemplateText;)V");
        extensionService.addInternalExtensionForTesting(new YamlExtension(this.getClass().getClassLoader(), props, false));
        extensionService.doStart();
        assertTrue("The internal extension should exist meaning the count should not be 0.", extensionService.getEnabledPointCuts().size() > 0);
    }

    @Test
    public void testNewExtensionsTakePrecedence() throws Exception {
        extensionService.start();
        Extension extension = createYamlExtension(EXTENSION_NAME, 1.0);
        Map<String, Extension> exMap = new HashMap<>();
        Extension validatedExtension = extensionService.validateExtension(extension, exMap);
        assertNotNull(validatedExtension);
        exMap.put(validatedExtension.getName(), validatedExtension);

        extension = createYamlExtension(EXTENSION_NAME, 2.0);
        validatedExtension = extensionService.validateExtension(extension, exMap);
        assertNotNull(validatedExtension);

        assertEquals(2.0, validatedExtension.getVersionNumber(), 0);
    }

    @Test
    public void testAddOld() throws Exception {
        extensionService.start();
        Extension extension = createYamlExtension(EXTENSION_NAME, 2.0);
        Map<String, Extension> exMap = new HashMap<>();
        Extension validatedExtension = extensionService.validateExtension(extension, exMap);
        assertNotNull(validatedExtension);
        assertEquals(2.0, validatedExtension.getVersionNumber(), 0);
        exMap.put(validatedExtension.getName(), validatedExtension);

        extension = createYamlExtension(EXTENSION_NAME, 1.0);
        validatedExtension = extensionService.validateExtension(extension, exMap);
        assertNull(validatedExtension);
    }

    @Test
    public void testFindExtensions() throws Exception {
        try {
            extensionService.start();
            List<ExtensionClassAndMethodMatcher> e = extensionService.getEnabledPointCuts();
            int beforeSize = e.size();
            File extDir = new File("/tmp/agentTestExtensions");
            if (!extDir.exists()) {
                extDir.mkdirs();
            }
            File extFile = new File(extDir, "test.yml");
            InputStream is = getClass().getResourceAsStream("test_extension.yml");
            FileOutputStream fileOutputStream = new FileOutputStream(extFile);
            Streams.copy(is, fileOutputStream);
            fileOutputStream.close();
            is.close();
            System.setProperty("newrelic.config.extensions.dir", extDir.getAbsolutePath());

            extensionService = new ExtensionService(serviceManager.getConfigService(), ExtensionsLoadedListener.NOOP);
            extensionService.start();
            e = extensionService.getEnabledPointCuts();
            assertEquals(beforeSize + 1, e.size());
        } finally {
            System.clearProperty("newrelic.config.extensions.dir");
        }
    }

    @Test
    public void testYMLExtensionIneFile() throws Exception {
        setupFiles(true, YML_FILE_PATH_1_1);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(1, pcs.size());

        List<String> classNames = new ArrayList<>();
        List<MethodMatcher> mms = new ArrayList<>();
        for (ExtensionClassAndMethodMatcher pc : pcs) {
            classNames.addAll(pc.getClassMatcher().getClassNames());
            assertEquals(ExactClassMatcher.class, pc.getClassMatcher().getClass());
            mms.add(pc.getMethodMatcher());
        }

        // verify that classes
        assertEquals(1, classNames.size());
        assertTrue(classNames.contains("java/lang/instrument/Instrumentation/Agent"));

        // verify method matcher
        assertEquals(1, mms.size());
        assertTrue(mms.get(0).matches(MethodMatcher.UNSPECIFIED_ACCESS, "getVersion", "()Ljava/lang/String;",
                com.google.common.collect.ImmutableSet.<String>of()));
    }

    /**
     * Tests that the extension service runs okay when there are no files present.
     */
    @Test
    public void testXMLExtensionNoFiles() throws Exception {
        setupFiles(true);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(0, pcs.size());
    }

    /**
     * Tests reading in the xml extensions for one file.
     */
    @Test
    public void testParamAttributes() throws Exception {
        setupFiles(true, XML_FILE_PATH_1);

        ExtensionService extService = new ExtensionService(configService, ExtensionsLoadedListener.NOOP);
        serviceManager.setExtensionService(extService);
        extService.start();

        List<ParameterAttributeName> parameterAttributeNames = null;
        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        for (ExtensionClassAndMethodMatcher matcher : pcs) {
            if (matcher.getClassMatcher().getClassNames().contains("test/CustomExtensionTest")
                    && matcher.getMethodMatcher().matches(0, "test2", "(Ljava/lang/String;J)V", null)) {
                parameterAttributeNames = matcher.getTraceDetails().getParameterAttributeNames();
            }
        }

        assertNotNull(parameterAttributeNames);
        assertEquals(2, parameterAttributeNames.size());
    }

    /**
     * Tests reading in the xml extensions for one file.
     */
    @Test
    public void testXMLExtensionOneFile() throws Exception {
        setupFiles(true, XML_FILE_PATH_1);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(3, pcs.size());

        List<String> classNames = new ArrayList<>();
        List<MethodMatcher> mms = new ArrayList<>();
        for (ExtensionClassAndMethodMatcher pc : pcs) {
            classNames.addAll(pc.getClassMatcher().getClassNames());
            mms.add(pc.getMethodMatcher());
        }
        // verify that classes
        assertEquals(3, classNames.size());
        assertTrue(classNames.contains("test/CustomExtensionTest$1"));
        assertTrue(classNames.contains("test/AbstractCustomExtensionTest"));
        assertTrue(classNames.contains("test/CustomExtensionTest"));

        // verify the methods
        assertEquals(3, mms.size());
        verifyMatch("run", "()V", mms);
        verifyMatch("abstractTest1", "()V", mms);
        verifyMatch("abstractTest2", "()V", mms);
        verifyMatch("test2", "(Ljava/lang/String;J)V", mms);
    }

    private void verifyMatch(String method, String signature, List<MethodMatcher> mms) {
        boolean wasMatch = false;
        for (MethodMatcher current : mms) {
            if (current.matches(MethodMatcher.UNSPECIFIED_ACCESS, method, signature,
                    com.google.common.collect.ImmutableSet.<String>of())) {
                wasMatch = true;
                return;
            }
        }
        assertTrue("One of the method matches should have matched " + method + signature + " but did not", wasMatch);
    }

    /**
     * Tests that only the xml with the higher version gets read in.
     */
    @Test
    public void testXMLExtensionDifferentVersions() throws Exception {
        setupFiles(true, XML_FILE_PATH_1, XML_FILE_PATH_1_1);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(3, pcs.size());

        List<String> classNames = new ArrayList<>();
        List<MethodMatcher> mms = new ArrayList<>();

        for (ExtensionClassAndMethodMatcher pc : pcs) {
            classNames.addAll(pc.getClassMatcher().getClassNames());
            mms.add(pc.getMethodMatcher());
        }
        // verify that classes
        assertEquals(3, classNames.size());
        assertTrue(classNames.contains("test/CustomExtensionTest$1"));
        assertTrue(classNames.contains("test/AbstractCustomExtensionTest1"));
        assertTrue(classNames.contains("test/CustomExtensionTest1"));

        // verify methods
        assertEquals(3, mms.size());
        verifyMatch("run", "()V", mms);
        verifyMatch("abstractTest11", "()V", mms);
        verifyMatch("abstractTest21", "()V", mms);
        verifyMatch("test2", "(Ljava/lang/String;I)V", mms);
    }

    /**
     * Verifies that the xml file gets read in instead of the yml since the files have the same name.
     */
    @Test
    public void testXMLExtensionWithYMLSameName() throws Exception {
        setupFiles(true, XML_FILE_PATH_1, YML_FILE_PATH_1_1);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(3, pcs.size());

        List<String> classNames = new ArrayList<>();

        for (ExtensionClassAndMethodMatcher pc : pcs) {
            classNames.addAll(pc.getClassMatcher().getClassNames());
        }
        // verify that classes
        assertEquals(3, classNames.size());
        assertTrue(classNames.contains("test/CustomExtensionTest$1"));
        assertTrue(classNames.contains("test/AbstractCustomExtensionTest"));
        assertTrue(classNames.contains("test/CustomExtensionTest"));
    }

    /**
     * The XML is invalid and so the parser should fail.
     *
     * @throws IOException
     */
    @Test
    public void testInvalidXML1() throws IOException {
        setupFiles(true, XML_INVALID_FORMAT);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(0, pcs.size());
    }

    /**
     * The XML is invalid and so the parser should fail for the first file.
     *
     * @throws IOException
     */
    @Test
    public void testInvalidXML2() throws IOException {
        setupFiles(true, XML_INVALID_FORMAT, XML_FILE_PATH_1);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(3, pcs.size());
    }

    /**
     * The first file should not be read in because it does not have a name or version.
     */
    @Test
    public void testXMLWithNoNameOrVersion1() throws Exception {
        setupFiles(true, XML_NO_NAME_OR_VERSION);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(0, pcs.size());
    }

    /**
     * The first file should not be read in because it does not have a name or version.
     */
    @Test
    public void testXMLWithNoNameOrVersion2() throws Exception {
        setupFiles(true, XML_NO_NAME_OR_VERSION, XML_FILE_PATH_1);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(3, pcs.size());

        List<String> classNames = new ArrayList<>();
        List<MethodMatcher> mms = new ArrayList<>();
        for (ExtensionClassAndMethodMatcher pc : pcs) {
            classNames.addAll(pc.getClassMatcher().getClassNames());
            mms.add(pc.getMethodMatcher());
        }
        // verify that classes
        assertEquals(3, classNames.size());
        assertTrue(classNames.contains("test/CustomExtensionTest$1"));
        assertTrue(classNames.contains("test/AbstractCustomExtensionTest"));
        assertTrue(classNames.contains("test/CustomExtensionTest"));
    }

    /**
     * Should not read in any of the point cuts.
     */
    @Test
    public void testXMLExtensionWithNoMethodPointCut() throws Exception {
        setupFiles(true, XML_NO_METHOD);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(0, pcs.size());
    }

    @Test
    public void testXMLExtensionWithDuplicates() throws Exception {
        setupFiles(true, XML_DUPS);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(3, pcs.size());

        List<String> classNames = new ArrayList<>();
        List<MethodMatcher> mms = new ArrayList<>();
        for (ExtensionClassAndMethodMatcher pc : pcs) {
            classNames.addAll(pc.getClassMatcher().getClassNames());
            mms.add(pc.getMethodMatcher());
        }

        // verify classes
        assertEquals(3, classNames.size());
        assertTrue(classNames.contains("test/TheClass"));
        assertTrue(classNames.contains("test/TheOtherClass"));

        // verify the methods
        assertEquals(3, mms.size());
        verifyMatch("run", "()Ljava/lang/String;", mms);
        verifyMatch("merge", "()Ljava/lang/String;", mms);
        verifyMatch("migrate", "()Ljava/lang/String;", mms);
        verifyMatch("run", "()Ljava/lang/String;", mms);
        verifyMatch("merge", "()Ljava/lang/String;", mms);
        verifyMatch("run", "(Ljava/lang/String;J)Ljava/lang/String;", mms);
    }

    @Test
    public void testXMLExtensionCaptureMethodParams() throws Exception {
        setupFiles(true, XML_CAPTURE_METHOD_PARAMS);

        List<ExtensionClassAndMethodMatcher> pcs = extensionService.getEnabledPointCuts();
        assertNotNull(pcs);
        assertEquals(1, pcs.size());

        List<String> classNames = new ArrayList<>();
        List<MethodMatcher> mms = new ArrayList<>();
        for (ExtensionClassAndMethodMatcher pc : pcs) {
            classNames.addAll(pc.getClassMatcher().getClassNames());
            mms.add(pc.getMethodMatcher());
        }

        // verify classes
        assertEquals(1, classNames.size());
        assertTrue(classNames.contains("com/nr/Client/impl/OkClient"));

        // verify the methods
        assertEquals(1, mms.size());
        verifyMatch("myMethod", "(Ljava/lang/String;J)V", mms);
    }

    @Test
    public void testReloadEmptyExtensions() {
        extensionService.doStart();
        Mockito.reset(serviceManager.getJmxService(),
                serviceManager.getClassTransformerService().getLocalRetransformer(),
                serviceManager.getClassTransformerService().getContextManager().getClassWeaverService());
        assertEquals(0, extensionService.getEnabledPointCuts().size());
        extensionService.afterHarvest(EXTENSION_NAME);
        assertEquals(0, extensionService.getEnabledPointCuts().size());
        Mockito.verifyNoMoreInteractions(serviceManager.getJmxService());
        Mockito.verifyNoMoreInteractions(serviceManager.getClassTransformerService().getLocalRetransformer());
        Mockito.verifyNoMoreInteractions(
                serviceManager.getClassTransformerService().getContextManager().getClassWeaverService());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testReloadXmlWithNewFile() throws IOException {
        ArgumentCaptor<Set> oldCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set> newCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);

        setupFiles(true);
        extensionService.doStart();
        Mockito.reset(serviceManager.getJmxService(),
                serviceManager.getClassTransformerService().getLocalRetransformer(),
                serviceManager.getClassTransformerService().getContextManager().getClassWeaverService());
        assertEquals(0, extensionService.getEnabledPointCuts().size());
        extensionService.afterHarvest(EXTENSION_NAME);
        moveFile(XML_FILE_PATH_1);
        assertEquals(0, extensionService.getEnabledPointCuts().size());
        // afterHarvest can be called multiple times
        extensionService.afterHarvest(EXTENSION_NAME);
        extensionService.afterHarvest(EXTENSION_NAME);
        assertEquals(3, extensionService.getEnabledPointCuts().size());

        verify(serviceManager.getJmxService()).reloadExtensions(oldCaptor.capture(), newCaptor.capture());
        assertEquals(0, oldCaptor.getValue().size());
        assertEquals(1, newCaptor.getValue().size());
        verify(serviceManager.getClassTransformerService().getLocalRetransformer()).setClassMethodMatchers(
                listCaptor.capture());
        assertEquals(3, listCaptor.getValue().size());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testNoReloadXmlWithUpdatedFileInFuture() throws Exception {
        ArgumentCaptor<Set> oldCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set> newCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);

        setupFiles(true, XML_FILE_PATH_1);
        assertEquals(3, extensionService.getEnabledPointCuts().size());
        verify(serviceManager.getJmxService()).reloadExtensions(oldCaptor.capture(), newCaptor.capture());
        assertEquals(0, oldCaptor.getValue().size());
        assertEquals(1, newCaptor.getValue().size());

        setFileTimestamp(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(48), XML_FILE_PATH_1); // update file timestamp to the future
        extensionService.afterHarvest(EXTENSION_NAME);
        assertEquals(3, extensionService.getEnabledPointCuts().size());

        // This should only happen once for the initial setup, not a second time due to the file timestamp being in the future
        verify(serviceManager.getJmxService(), Mockito.times(1)).reloadExtensions(oldCaptor.capture(),
                newCaptor.capture());
        assertEquals(0, oldCaptor.getValue().size());
        assertEquals(1, newCaptor.getValue().size());
        verify(serviceManager.getClassTransformerService().getLocalRetransformer(), Mockito.times(1))
                .setClassMethodMatchers(listCaptor.capture());
        assertEquals(3, listCaptor.getValue().size());

        setupFiles(false); // reset extension dir
        extensionService.afterHarvest(EXTENSION_NAME); // should remove extensions.
        assertEquals(0, extensionService.getEnabledPointCuts().size());

        oldCaptor = ArgumentCaptor.forClass(Set.class);
        newCaptor = ArgumentCaptor.forClass(Set.class);
        verify(serviceManager.getJmxService(), Mockito.times(2)).reloadExtensions(oldCaptor.capture(),
                newCaptor.capture());
        assertEquals(1, oldCaptor.getValue().size());
        assertEquals(0, newCaptor.getValue().size());
        listCaptor = ArgumentCaptor.forClass(List.class);
        verify(serviceManager.getClassTransformerService().getLocalRetransformer(), Mockito.times(2))
                .setClassMethodMatchers(listCaptor.capture());
        assertEquals(0, listCaptor.getValue().size());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testReloadXmlWithUpdatedFile() throws Exception {
        ArgumentCaptor<Set> oldCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set> newCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);

        setupFiles(true, XML_FILE_PATH_1);
        assertEquals(3, extensionService.getEnabledPointCuts().size());
        verify(serviceManager.getJmxService()).reloadExtensions(oldCaptor.capture(), newCaptor.capture());
        assertEquals(0, oldCaptor.getValue().size());
        assertEquals(1, newCaptor.getValue().size());

        moveFile(XML_FILE_PATH_1); // update file timestamp
        extensionService.afterHarvest(EXTENSION_NAME);
        assertEquals(3, extensionService.getEnabledPointCuts().size());

        verify(serviceManager.getJmxService(), Mockito.times(2)).reloadExtensions(oldCaptor.capture(),
                newCaptor.capture());
        assertEquals(1, oldCaptor.getValue().size());
        assertEquals(1, newCaptor.getValue().size());
        verify(serviceManager.getClassTransformerService().getLocalRetransformer(), Mockito.times(2))
                .setClassMethodMatchers(listCaptor.capture());
        assertEquals(3, listCaptor.getValue().size());

        setupFiles(false); // reset extension dir
        extensionService.afterHarvest(EXTENSION_NAME); // should remove extensions.
        assertEquals(0, extensionService.getEnabledPointCuts().size());

        oldCaptor = ArgumentCaptor.forClass(Set.class);
        newCaptor = ArgumentCaptor.forClass(Set.class);
        verify(serviceManager.getJmxService(), Mockito.times(3)).reloadExtensions(oldCaptor.capture(),
                newCaptor.capture());
        assertEquals(1, oldCaptor.getValue().size());
        assertEquals(0, newCaptor.getValue().size());
        listCaptor = ArgumentCaptor.forClass(List.class);
        verify(serviceManager.getClassTransformerService().getLocalRetransformer(), Mockito.times(3))
                .setClassMethodMatchers(listCaptor.capture());
        assertEquals(0, listCaptor.getValue().size());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testReloadXmlWithAddedFile() throws IOException {
        ArgumentCaptor<Set> oldCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set> newCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);

        setupFiles(true, XML_FILE_PATH_1);
        assertEquals(3, extensionService.getEnabledPointCuts().size());

        moveFile(XML_FILE_PATH_1_1); // update file timestamp
        extensionService.afterHarvest(EXTENSION_NAME);
        assertEquals(3, extensionService.getEnabledPointCuts().size());
        verify(serviceManager.getJmxService(), Mockito.times(2)).reloadExtensions(oldCaptor.capture(),
                newCaptor.capture());
        assertEquals(1, oldCaptor.getValue().size());
        assertEquals(1, newCaptor.getValue().size());
        verify(serviceManager.getClassTransformerService().getLocalRetransformer(), Mockito.times(2))
                .setClassMethodMatchers(listCaptor.capture());
        assertEquals(3, listCaptor.getValue().size());

        setupFiles(false, XML_FILE_PATH_1_1); // removes original extension
        extensionService.afterHarvest(EXTENSION_NAME); // should update extensions.
        assertEquals(3, extensionService.getEnabledPointCuts().size());

        oldCaptor = ArgumentCaptor.forClass(Set.class);
        newCaptor = ArgumentCaptor.forClass(Set.class);
        verify(serviceManager.getJmxService(), Mockito.times(3)).reloadExtensions(oldCaptor.capture(),
                newCaptor.capture());
        assertEquals(1, oldCaptor.getValue().size());
        assertEquals(1, newCaptor.getValue().size());
        listCaptor = ArgumentCaptor.forClass(List.class);
        verify(serviceManager.getClassTransformerService().getLocalRetransformer(), Mockito.times(3))
                .setClassMethodMatchers(listCaptor.capture());
        assertEquals(3, listCaptor.getValue().size());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testReloadWeaveJarWithNewFile() throws IOException {
        ArgumentCaptor<Collection> fileCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> fileCaptor2 = ArgumentCaptor.forClass(Collection.class);

        setupFiles(true);
        assertEquals(0, extensionService.getWeaveExtensions().size());
        extensionService.afterHarvest(EXTENSION_NAME);
        moveFile(WEAVE_INSTRUMENTATION);
        assertEquals(0, extensionService.getWeaveExtensions().size());
        // afterHarvest can be called multiple times
        extensionService.afterHarvest(EXTENSION_NAME);
        extensionService.afterHarvest(EXTENSION_NAME);
        assertEquals(1, extensionService.getWeaveExtensions().size());

        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService())
                .reloadExternalWeavePackages(fileCaptor.capture(), fileCaptor2.capture());
        assertEquals(1, fileCaptor.getValue().size());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testReloadWeaveJarWithUpdatedFile() throws IOException {
        ArgumentCaptor<Collection> fileCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> fileCaptor2 = ArgumentCaptor.forClass(Collection.class);

        setupFiles(true, WEAVE_INSTRUMENTATION);
        assertEquals(1, extensionService.getWeaveExtensions().size());

        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService()).reloadExternalWeavePackages(
                fileCaptor.capture(), fileCaptor2.capture());
        assertEquals(1, fileCaptor.getValue().size());
        assertEquals(0, fileCaptor2.getValue().size());

        moveFile(WEAVE_INSTRUMENTATION); // update file timestamp
        extensionService.afterHarvest(EXTENSION_NAME);
        assertEquals(1, extensionService.getWeaveExtensions().size());

        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService(), Mockito.times(2)).reloadExternalWeavePackages(
                fileCaptor.capture(), fileCaptor2.capture());
        assertEquals(1, fileCaptor.getValue().size());
        assertEquals(0, fileCaptor2.getValue().size());

        setupFiles(false); // reset extension dir
        extensionService.afterHarvest(EXTENSION_NAME); // should remove extensions.
        assertEquals(0, extensionService.getWeaveExtensions().size());

        fileCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService(), Mockito.times(3)).reloadExternalWeavePackages(
                fileCaptor.capture(), fileCaptor2.capture());
        assertEquals(0, fileCaptor.getValue().size());
        assertEquals(1, fileCaptor2.getValue().size());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testNoReloadWeaveJarWithUpdatedFileInFuture() throws IOException {
        ArgumentCaptor<Collection> fileCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> fileCaptor2 = ArgumentCaptor.forClass(Collection.class);

        // set file timestamp to the future
        setupFiles(true, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(48), WEAVE_INSTRUMENTATION);
        assertEquals(1, extensionService.getWeaveExtensions().size());

        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService()).reloadExternalWeavePackages(
                fileCaptor.capture(), fileCaptor2.capture());
        assertEquals(1, fileCaptor.getValue().size());
        assertEquals(0, fileCaptor2.getValue().size());

        setFileTimestamp(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(48), WEAVE_INSTRUMENTATION); // update file timestamp to the future
        extensionService.afterHarvest(EXTENSION_NAME);
        assertEquals(1, extensionService.getWeaveExtensions().size());

        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService(), Mockito.times(1)).reloadExternalWeavePackages(
                fileCaptor.capture(), fileCaptor2.capture());
        assertEquals(1, fileCaptor.getValue().size());
        assertEquals(0, fileCaptor2.getValue().size());

        setupFiles(false); // reset extension dir
        extensionService.afterHarvest(EXTENSION_NAME); // should remove extensions.
        assertEquals(0, extensionService.getWeaveExtensions().size());

        fileCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService(), Mockito.times(2)).reloadExternalWeavePackages(
                fileCaptor.capture(), fileCaptor2.capture());
        assertEquals(0, fileCaptor.getValue().size());
        assertEquals(1, fileCaptor2.getValue().size());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testReloadWeaveJarWithAddedFile() throws IOException {
        ArgumentCaptor<Collection> fileCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> fileCaptor2 = ArgumentCaptor.forClass(Collection.class);

        setupFiles(true, WEAVE_INSTRUMENTATION);
        assertEquals(1, extensionService.getWeaveExtensions().size());

        moveFile(WEAVE_INSTRUMENTATION_2); // Add New Extension
        extensionService.afterHarvest(EXTENSION_NAME);
        assertEquals("Services: " + extensionService.getWeaveExtensions().iterator().next(), 2,
                extensionService.getWeaveExtensions().size());
        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService(),
                Mockito.times(2)).reloadExternalWeavePackages(fileCaptor.capture(), fileCaptor2.capture());
        assertEquals(1, fileCaptor.getValue().size());

        setupFiles(false, WEAVE_INSTRUMENTATION_2); // touch extension file
        extensionService.afterHarvest(EXTENSION_NAME); // should update extensions.
        assertEquals(1, extensionService.getWeaveExtensions().size());

        fileCaptor = ArgumentCaptor.forClass(Collection.class);
        fileCaptor2 = ArgumentCaptor.forClass(Collection.class);
        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService(),
                Mockito.times(3)).reloadExternalWeavePackages(fileCaptor.capture(), fileCaptor2.capture());
        assertEquals(1, fileCaptor.getValue().size());
    }

    @Test
    public void testDisabledReloadWeaveJarWithAddedFile() throws IOException {
        updateConfigToDisableReload();

        ArgumentCaptor<Collection> collectionCaptor = ArgumentCaptor.forClass(Collection.class);

        setupFiles(true, WEAVE_INSTRUMENTATION);
        assertEquals(1, extensionService.getWeaveExtensions().size());

        moveFile(WEAVE_INSTRUMENTATION_2); // Add New Extension
        extensionService.afterHarvest(EXTENSION_NAME);
        assertEquals("Services: " + extensionService.getWeaveExtensions().iterator().next(), 1,
                extensionService.getWeaveExtensions().size());
        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService(),
                Mockito.times(1)).reloadExternalWeavePackages(collectionCaptor.capture(), any(Collection.class));
        assertEquals(1, collectionCaptor.getValue().size());

        setupFiles(false, WEAVE_INSTRUMENTATION_2); // touch extension file
        extensionService.afterHarvest(EXTENSION_NAME); // should update extensions.
        assertEquals(1, extensionService.getWeaveExtensions().size());

        collectionCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService(),
                Mockito.times(1)).reloadExternalWeavePackages(collectionCaptor.capture(), any(Collection.class));
        assertEquals(1, collectionCaptor.getValue().size());
    }

    @Test
    public void testDisabledReloadWeaveJarWithUpdatedFile() throws IOException {
        updateConfigToDisableReload();

        ArgumentCaptor<Collection> collectionCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> collectionCaptor2 = ArgumentCaptor.forClass(Collection.class);

        setupFiles(true, WEAVE_INSTRUMENTATION);
        assertEquals(1, extensionService.getWeaveExtensions().size());

        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService()).reloadExternalWeavePackages(
                collectionCaptor.capture(), collectionCaptor2.capture());
        assertEquals(1, collectionCaptor.getValue().size());
        assertEquals(0, collectionCaptor2.getValue().size());

        moveFile(WEAVE_INSTRUMENTATION); // update file timestamp
        extensionService.afterHarvest(EXTENSION_NAME);
        assertEquals(1, extensionService.getWeaveExtensions().size());

        verify(serviceManager.getClassTransformerService().getContextManager().getClassWeaverService(), Mockito.times(1)).reloadExternalWeavePackages(
                collectionCaptor.capture(), collectionCaptor2.capture());
        assertEquals(1, collectionCaptor.getValue().size());
        assertEquals(0, collectionCaptor2.getValue().size());
    }

    private void updateConfigToDisableReload() {
        Map<String, Boolean> extensionsConfig = Collections.singletonMap(ExtensionsConfigImpl.RELOAD_MODIFIED, false);

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(AgentConfigImpl.APP_NAME, EXTENSION_NAME);
        configMap.put(AgentConfigImpl.EXTENSIONS, extensionsConfig);

        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        configService = ConfigServiceFactory.createConfigService(config, Collections.<String, Object>emptyMap());
        extensionService = new ExtensionService(configService, ExtensionsLoadedListener.NOOP);
        serviceManager.setExtensionService(extensionService);
    }

    /**
     * Sets up the extension files.
     *
     * @param startService whether or not to start the extension service
     * @param inPaths Name of the config file to read in and move to extension directory.
     * @throws IOException
     */
    private void setupFiles(boolean startService, String... inPaths) throws IOException {
        // create the directory
        File dir = File.createTempFile(getClass().getName(), "-WorkDir");
        dir.delete();
        dir.mkdir();
        dir.deleteOnExit();

        // set the system property
        System.setProperty("newrelic.config.extensions.dir", dir.getPath());

        // move files to the extension directory
        moveFile(inPaths);

        if (startService) {
            try {
                extensionService.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setupFiles(boolean startService, long timestamp, String... inPaths) throws IOException {
        // create the directory
        File dir = File.createTempFile(getClass().getName(), "-WorkDir");
        dir.delete();
        dir.mkdir();
        dir.deleteOnExit();

        // set the system property
        System.setProperty("newrelic.config.extensions.dir", dir.getPath());

        // move files to the extension directory
        setFileTimestamp(timestamp, inPaths);

        if (startService) {
            try {
                extensionService.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Change the last modified timestamp of the existing file to a value that's greater than when the extension
     * service last reloaded it. Setting the lastModified value of a file may potentially truncate that value to
     * the nearest second. Therefore, we add 1 second to the lastModified time until it exceeds the last time
     * the extension service reloaded it.
     *
     * @param file
     * @see File#lastModified()
     */
    private void touchFile(File file) {
        while (file.lastModified() < extensionService.getLastReloaded()) {
            long newLastModified = file.lastModified() + 1000L;
            file.setLastModified(newLastModified);
        }

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < startTime + 1000L) {
            // Wait for 1+ second to ensure we don't try to set a file modification date in the future
        }
    }

    private void writeFile(String resource, File file) {
        // put the file in the directory
        InputStream fis = getClass().getResourceAsStream(resource);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            boolean reading = true;
            while (reading) {
                int size = fis.available();
                byte[] bytes = new byte[size];
                int amountRead = fis.read(bytes);
                reading = amountRead > 0;
                fos.write(bytes);
            }
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("File not found. Test not set up correctly.");
        } catch (IOException e) {
            e.printStackTrace();
            fail("Error reading in file. " + e.getMessage());
        } finally {
            closeQuietly(fis);
            closeQuietly(fos);
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * Copies the file to the extension directory.
     *
     * @param names The names of the file.
     */
    private void moveFile(String... names) {
        for (String name : names) {
            File out = new File(System.getProperty("newrelic.config.extensions.dir") + "/" + name);
            if (out.exists()) {
                touchFile(out);
            } else {
                writeFile(name, out);
            }
        }
    }

    private void setFileTimestamp(long newTimestamp, String... names) {
        for (String name : names) {
            File out = new File(System.getProperty("newrelic.config.extensions.dir") + "/" + name);
            if (!out.exists()) {
                writeFile(name, out);
                out = new File(System.getProperty("newrelic.config.extensions.dir") + "/" + name);
            }
            out.setLastModified(newTimestamp);
        }
    }

    private Extension createYamlExtension(String name, final double version) {
        Map<String, Object> props = new HashMap<>();
        props.put("version", version);
        return new YamlExtension(this.getClass().getClassLoader(), name, props, true);
    }
}
