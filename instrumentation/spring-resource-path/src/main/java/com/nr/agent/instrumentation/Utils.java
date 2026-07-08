package com.nr.agent.instrumentation;

import org.springframework.core.io.Resource;

import java.util.Arrays;

public class Utils {
    private static final String NR_SHADOWED_DEPENDENCIES_URL_PREFIX = "com/newrelic/agent/deps";

    /**
     * Filters out New Relic agent shadowed dependency resources from the given resource array.
     * This forces New Relic agent shadowed dependencies to become undiscoverable by Spring.
     *
     * @param resources the array of resources to filter, may be null
     * @return a new array excluding resources that match the shadowed dependencies pattern,
     *         or the original array if it was null
     */
    public static Resource[] excludeNRShadowedDependencies(Resource[] resources) {
        if (resources == null) {
            return null;
        }
       return Arrays.stream(resources)
               .filter(resource -> !isShadowedDependency(resource))
               .toArray(Resource[]::new);
    }

    private static boolean isShadowedDependency(Resource resource) {
        return resource.getDescription().contains(NR_SHADOWED_DEPENDENCIES_URL_PREFIX);
    }
}
