package org.jboss.modules;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jboss.modules.JBossUtils;

import java.util.Set;
import java.util.jar.Attributes;

@Weave(originalName = "org.jboss.modules.FileSystemClassPathModuleFinder")
public class FileSystemClassPathModuleFinder_Instrumentation {

    void addModuleDependencies(ModuleSpec.Builder builder, ModuleLoader fatModuleLoader, Attributes mainAttributes) {
        Weaver.callOriginal();
        Set<String> dependencyNames = JBossUtils.getDependencyNames(builder);
        JBossUtils.addModuleDependencies("java.logging", dependencyNames, builder, fatModuleLoader);
        JBossUtils.addModuleDependencies("java.management", dependencyNames, builder, fatModuleLoader);
    }

}
