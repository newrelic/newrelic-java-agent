package com.newrelic.agent.service.module;

import com.newrelic.agent.extension.ExtensionsLoadedListener;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.api.agent.Logger;

import java.util.concurrent.ExecutorService;

public class JarCollectorInputs {
    private final ExtensionsLoadedListener extensionAnalysisProducer;
    private final ClassMatchVisitorFactory classNoticingFactory;

    public JarCollectorInputs(boolean jarCollectorEnabled, JarAnalystFactory jarAnalystFactory, ExecutorService executorService,
            Logger jarCollectorLogger) {
        classNoticingFactory = jarCollectorEnabled
                ? new ClassNoticingFactory(jarAnalystFactory, executorService, jarCollectorLogger)
                : ClassMatchVisitorFactory.NO_OP_FACTORY;

        extensionAnalysisProducer = jarCollectorEnabled
                ? new ExtensionAnalysisProducer(jarAnalystFactory, executorService, jarCollectorLogger)
                : ExtensionsLoadedListener.NOOP;
    }

    public ExtensionsLoadedListener getExtensionAnalysisProducer() {
        return extensionAnalysisProducer;
    }

    public ClassMatchVisitorFactory getClassNoticingFactory() {
        return classNoticingFactory;
    }
}
