package com.newrelic.agent.service.module;

import com.newrelic.agent.extension.ExtensionServiceTest;
import com.newrelic.api.agent.Logger;
import com.newrelic.weave.weavepackage.WeavePackageConfig;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class WeavePackageAnalyzerTest {
    @Test
    public void weavePackageConstructionReadsAttributesFromJars() {
        URL url = getClass().getResource('/' + ExtensionServiceTest.class.getPackage().getName().replace('.', '/') + '/'
                + ExtensionServiceTest.WEAVE_INSTRUMENTATION);

        final List<JarData> jarData = new ArrayList<>();
        Consumer<JarData> mockConsumer = new Consumer<JarData>() {
            @Override
            public void accept(JarData x) {
                jarData.add(x);
            }
        };

        WeavePackageAnalyzer target = new WeavePackageAnalyzer(new File(url.getFile()), mockConsumer, mock(Logger.class));
        target.run();

        assertEquals(1, jarData.size());
        assertEquals("com.newrelic.instrumentation.spring-jms-2", jarData.get(0).getName());
        assertEquals("1.0", jarData.get(0).getVersion());

        assertEquals(url.toString(), jarData.get(0).getJarInfo().attributes.get("weaveFile"));
        assertEquals("10ce178a632add8d5a98442a9cf1220f34c95874", jarData.get(0).getJarInfo().attributes.get("sha1Checksum"));
    }

    @Test
    public void usesWeavePackageConfigForAttributes() {
        URL url = getClass().getResource('/' + ExtensionServiceTest.class.getPackage().getName().replace('.', '/') + '/'
                + ExtensionServiceTest.WEAVE_INSTRUMENTATION);

        Consumer<JarData> mockConsumer = new Consumer<JarData>() {
            @Override
            public void accept(JarData x) {
            }
        };

        WeavePackageAnalyzer target = new WeavePackageAnalyzer(new File(url.getFile()), mockConsumer, mock(Logger.class));
        JarData jarData = target.getWeaveJar(
                new File(url.getFile()),
                WeavePackageConfig.builder().name("jms").version(6.66f).source(url.getPath()).build());

        assertEquals("jms", jarData.getName());
        assertEquals("6.66", jarData.getVersion());

        assertEquals(url.getFile(), jarData.getJarInfo().attributes.get("weaveFile"));
        assertEquals("10ce178a632add8d5a98442a9cf1220f34c95874", jarData.getJarInfo().attributes.get("sha1Checksum"));
    }
}
