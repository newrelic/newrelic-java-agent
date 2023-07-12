package com.newrelic.agent.config;

import org.junit.Assert;
import org.junit.Test;

public class IBMUtilsTest {
    @Test
    public void isIbmJVM_returnsProperValue_basedOnSysProperty() {
        System.setProperty("java.vendor", "IBM Corporation");
        Assert.assertTrue(IBMUtils.isIbmJVM());

        System.setProperty("java.vendor", "foo");
        Assert.assertFalse(IBMUtils.isIbmJVM());
    }
}
