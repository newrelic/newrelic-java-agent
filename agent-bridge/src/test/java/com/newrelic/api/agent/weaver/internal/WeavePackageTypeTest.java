package com.newrelic.api.agent.weaver.internal;

import org.junit.Assert;
import org.junit.Test;

public class WeavePackageTypeTest {
    @Test
    public void isInternal_withInternalPackageType_returnsTrue() {
        Assert.assertTrue(WeavePackageType.INTERNAL.isInternal());
    }

    @Test
    public void isInternal_withNonInternalPackageType_returnsFalse() {
        Assert.assertTrue(WeavePackageType.CUSTOM.isInternal());
        Assert.assertTrue(WeavePackageType.FIELD.isInternal());
        Assert.assertTrue(WeavePackageType.UNKNOWN.isInternal());
    }

    @Test
    public void getSupportabilityMetric_returnsProperValue() {
        Assert.assertEquals("Supportability/API/foo/Internal", WeavePackageType.INTERNAL.getSupportabilityMetric("foo"));
        Assert.assertEquals("Supportability/API/foo/Custom", WeavePackageType.CUSTOM.getSupportabilityMetric("foo"));
        Assert.assertEquals("Supportability/API/foo/Field", WeavePackageType.FIELD.getSupportabilityMetric("foo"));
        Assert.assertEquals("Supportability/API/foo/Unknown", WeavePackageType.UNKNOWN.getSupportabilityMetric("foo"));
    }
}
