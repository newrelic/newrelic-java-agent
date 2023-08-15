package com.newrelic.weave;

import com.google.common.collect.Queues;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Queue;

/**
 * A class used to filter specific violations from being added to {@link com.newrelic.weave.weavepackage.PackageValidationResult} instances.
 * Instances of this class are created and stored with each {@link com.newrelic.weave.weavepackage.WeavePackageConfig} object (if
 * filters are configured for the instrumentation package).
 */
public class WeaveViolationFilter {
    private final Collection<WeaveViolationType> typesToFilter = EnumSet.noneOf(WeaveViolationType.class);

    private final String weavePackage;

    public WeaveViolationFilter(String weavePackage, Collection<WeaveViolationType> types) {
        this.typesToFilter.addAll(types);
        this.weavePackage = weavePackage;
    }

    /**
     * Remove ignorable {@link WeaveViolation} instances from the supplied collection of violations.
     *
     * @param violations the collection of WeaveViolations to apply the filter to
     * @return a filtered collection of WeaveViolations
     */
    public Collection<WeaveViolation> filterViolationCollection(Collection<WeaveViolation> violations) {
        Queue<WeaveViolation> newViolationCollection = Queues.newConcurrentLinkedQueue();
        for (WeaveViolation violation : violations) {
            if (!shouldIgnoreViolation(violation)) {
                newViolationCollection.add(violation);
            }
        }

        return  newViolationCollection;
    }

    /**
     * Return true if the supplied {@link WeaveViolation} instance should be ignored based on the filter
     *
     * @param violation the WeaveViolation to check
     * @return true if this WeaveViolation should be filtered
     */
    public boolean shouldIgnoreViolation(WeaveViolation violation) {
        return typesToFilter.contains(violation.getType());
    }

    /**
     * Return the weave package this filter belongs to
     *
     * @return the weave package name
     */
    public String getWeavePackage() {
        return this.weavePackage;
    }
}
