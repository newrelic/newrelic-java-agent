package com.newrelic.agent.service.module;

import com.newrelic.api.agent.Logger;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class URLAnalyzerTest {

    @Test
    public void testRunProcesses() throws Exception {
        JarData jarData = new JarData("foo", new JarInfo("1.2.3", Collections.<String, String>emptyMap()));
        URL url = URI.create("https://example.com").toURL();
        Logger logger = mock(Logger.class);
        Function<URL, JarData> processor = mock(Function.class);
        Consumer<JarData> consumer = mock(Consumer.class);

        when(processor.apply(url)).thenReturn(jarData);

        URLAnalyzer testClass = new URLAnalyzer(url, processor, consumer, logger);
        testClass.run();
        verify(consumer).accept(jarData);
    }

    @Test
    public void testProcessingSkipsNull() throws Exception {
        URL url = URI.create("https://example.com").toURL();
        Logger logger = mock(Logger.class);
        Function<URL, JarData> processor = mock(Function.class);
        Consumer<JarData> consumer = mock(Consumer.class);

        when(processor.apply(url)).thenReturn(null);

        URLAnalyzer testClass = new URLAnalyzer(url, processor, consumer, logger);
        testClass.run();
        verifyNoMoreInteractions(consumer);
    }

}
