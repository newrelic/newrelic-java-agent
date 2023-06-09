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
        Assert.assertFalse(WeavePackageType.CUSTOM.isInternal());
        Assert.assertFalse(WeavePackageType.FIELD.isInternal());
        Assert.assertFalse(WeavePackageType.UNKNOWN.isInternal());
    }

    @Test
    public void getSupportabilityMetric_returnsProperValue() {
        Assert.assertEquals("Supportability/API/foo/Internal", WeavePackageType.INTERNAL.getSupportabilityMetric("foo"));
        Assert.assertEquals("Supportability/API/foo/Custom", WeavePackageType.CUSTOM.getSupportabilityMetric("foo"));
        Assert.assertEquals("Supportability/API/foo/Field", WeavePackageType.FIELD.getSupportabilityMetric("foo"));
        Assert.assertEquals("Supportability/API/foo/API", WeavePackageType.UNKNOWN.getSupportabilityMetric("foo"));
    }
}
