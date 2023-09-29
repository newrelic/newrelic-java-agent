package com.nr.agent.instrumentation.jboss.modules;

import com.newrelic.api.agent.NewRelic;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleDependencySpec;
import org.jboss.modules.ModuleDependencySpecBuilder;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class JBossUtils {
    private JBossUtils() {}

    public static Set<String> getDependencyNames(ModuleSpec.Builder builder) {
        List<DependencySpec> dependencies = new ArrayList<>(0);
        try {
            Field f = builder.getClass().getDeclaredField("dependencies");
            f.setAccessible(true);
            dependencies = (List<DependencySpec>) f.get(builder);
        }
        catch ( NoSuchFieldException ex) {
            // field doesn't exist
        } catch (IllegalAccessException | ClassCastException ex) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Failed to access dependencies field from jboss-modules", ex);
        }
        Set<String> dependencyNames = new HashSet<>(dependencies.size());
        for (DependencySpec spec: dependencies) {
            if (spec instanceof ModuleDependencySpec) {
                dependencyNames.add(((ModuleDependencySpec) spec).getName());
            }
        }
        return dependencyNames;
    }

    public static void addModuleDependencies(String name, Set<String> addedDependencies, ModuleSpec.Builder builder, ModuleLoader fatModuleLoader) {

        if (addedDependencies.contains(name)) {
            return;
        }

        builder.addDependency(new ModuleDependencySpecBuilder()
                .setImportServices(false)
                .setExport(false)
                .setModuleLoader(fatModuleLoader)
                .setName(name)
                .setOptional(false)
                .build());
    }
}
