package com.newrelic.agent.service.module;

import com.newrelic.agent.extension.ExtensionsLoadedListener;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.api.agent.Logger;

import java.util.concurrent.ExecutorService;

public class JarCollectorInputs {
    private final ExtensionsLoadedListener extensionAnalysisProducer;
    private final ClassMatchVisitorFactory classNoticingFactory;

    JarCollectorInputs(ExtensionsLoadedListener extensionAnalysisProducer, ClassMatchVisitorFactory classNoticingFactory) {
        this.extensionAnalysisProducer = extensionAnalysisProducer;
        this.classNoticingFactory = classNoticingFactory;
    }

    public static JarCollectorInputs build(boolean jarCollectorEnabled, JarAnalystFactory jarAnalystFactory, ExecutorService executorService,
                              Logger jarCollectorLogger) {
        ClassMatchVisitorFactory classNoticingFactory = jarCollectorEnabled
                ? new ClassNoticingFactory(jarAnalystFactory, executorService, jarCollectorLogger)
                : ClassMatchVisitorFactory.NO_OP_FACTORY;

        ExtensionsLoadedListener extensionAnalysisProducer = jarCollectorEnabled
                ? new ExtensionAnalysisProducer(jarAnalystFactory, executorService, jarCollectorLogger)
                : ExtensionsLoadedListener.NOOP;
        return new JarCollectorInputs(extensionAnalysisProducer, classNoticingFactory);
    }

    public ExtensionsLoadedListener getExtensionAnalysisProducer() {
        return extensionAnalysisProducer;
    }

    public ClassMatchVisitorFactory getClassNoticingFactory() {
        return classNoticingFactory;
    }
}
