package com.newrelic.agent.service.module;

import com.newrelic.agent.extension.ExtensionsLoadedListener;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.api.agent.Logger;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class JarCollectorInputsTest {
    @Test
    public void correctInputsWhenEnabled() {
        JarCollectorInputs target = JarCollectorInputs.build(true, mock(JarAnalystFactory.class), mock(ExecutorService.class), mock(Logger.class));
        assertTrue(target.getClassToJarPathSubmitter() instanceof ClassToJarPathSubmitterImpl);
        assertTrue(target.getExtensionAnalysisProducer() instanceof ExtensionAnalysisProducer);
        assertNotSame(ClassToJarPathSubmitterImpl.NO_OP_INSTANCE, target.getClassToJarPathSubmitter());
        assertNotSame(ExtensionsLoadedListener.NOOP, target.getExtensionAnalysisProducer());
    }

    @Test
    public void correctInputsWhenNotEnabled() {
        JarCollectorInputs target = JarCollectorInputs.build(false, mock(JarAnalystFactory.class), mock(ExecutorService.class), mock(Logger.class));
        assertFalse(target.getClassToJarPathSubmitter() instanceof ClassToJarPathSubmitterImpl);
        assertFalse(target.getExtensionAnalysisProducer() instanceof ExtensionAnalysisProducer);
        assertSame(ClassToJarPathSubmitterImpl.NO_OP_INSTANCE, target.getClassToJarPathSubmitter());
        assertSame(ExtensionsLoadedListener.NOOP, target.getExtensionAnalysisProducer());
    }

}