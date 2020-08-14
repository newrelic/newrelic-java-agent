package com.newrelic.agent.service.module;

import com.newrelic.agent.extension.ExtensionsLoadedListener;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.api.agent.Logger;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class JarCollectorInputsTest {
    @Test
    public void correctInputsWhenEnabled() {
        JarCollectorInputs target = new JarCollectorInputs(true, mock(JarAnalystFactory.class), mock(ExecutorService.class), mock(Logger.class));
        assertTrue(target.getClassNoticingFactory() instanceof ClassNoticingFactory);
        assertTrue(target.getExtensionAnalysisProducer() instanceof ExtensionAnalysisProducer);
        assertNotSame(ClassMatchVisitorFactory.NO_OP_FACTORY, target.getClassNoticingFactory());
        assertNotSame(ExtensionsLoadedListener.NOOP, target.getExtensionAnalysisProducer());
    }

    @Test
    public void correctInputsWhenNotEnabled() {
        JarCollectorInputs target = new JarCollectorInputs(false, mock(JarAnalystFactory.class), mock(ExecutorService.class), mock(Logger.class));
        assertFalse(target.getClassNoticingFactory() instanceof ClassNoticingFactory);
        assertFalse(target.getExtensionAnalysisProducer() instanceof ExtensionAnalysisProducer);
        assertSame(ClassMatchVisitorFactory.NO_OP_FACTORY, target.getClassNoticingFactory());
        assertSame(ExtensionsLoadedListener.NOOP, target.getExtensionAnalysisProducer());
    }

}