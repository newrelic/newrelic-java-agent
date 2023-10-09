package com.newrelic.agent.service.module;

import com.newrelic.agent.extension.ExtensionsLoadedListener;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.api.agent.Logger;

import java.util.concurrent.ExecutorService;

public class JarCollectorInputs {
    private final ExtensionsLoadedListener extensionAnalysisProducer;
    private final ClassToJarPathSubmitter classToJarPathSubmitter;

    JarCollectorInputs(ExtensionsLoadedListener extensionAnalysisProducer, ClassToJarPathSubmitter classToJarPathSubmitter) {
        this.extensionAnalysisProducer = extensionAnalysisProducer;
        this.classToJarPathSubmitter = classToJarPathSubmitter;
    }

    public static JarCollectorInputs build(boolean jarCollectorEnabled, JarAnalystFactory jarAnalystFactory, ExecutorService executorService,
                              Logger jarCollectorLogger) {
        ClassToJarPathSubmitter classToJarPathSubmitter = jarCollectorEnabled
                ? new ClassToJarPathSubmitterImpl(jarAnalystFactory, executorService, jarCollectorLogger)
                : ClassToJarPathSubmitterImpl.NO_OP_INSTANCE;

        ExtensionsLoadedListener extensionAnalysisProducer = jarCollectorEnabled
                ? new ExtensionAnalysisProducer(jarAnalystFactory, executorService, jarCollectorLogger)
                : ExtensionsLoadedListener.NOOP;
        return new JarCollectorInputs(extensionAnalysisProducer, classToJarPathSubmitter);
    }

    public ExtensionsLoadedListener getExtensionAnalysisProducer() {
        return extensionAnalysisProducer;
    }

    public ClassToJarPathSubmitter getClassToJarPathSubmitter() {
        return classToJarPathSubmitter;
    }
}
