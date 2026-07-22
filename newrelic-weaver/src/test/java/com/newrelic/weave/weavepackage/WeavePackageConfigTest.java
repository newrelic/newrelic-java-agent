package com.newrelic.weave.weavepackage;

import com.newrelic.weave.WeaveViolationFilter;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WeavePackageConfigTest {
    @Test
    public void ConfigBuilder_withConfiguredFilters_createsWeaveViolationFilter() {
        WeavePackageConfig.Builder builder = new WeavePackageConfig.Builder();
        WeavePackageConfig config = builder.name("test")
                .weaveViolationFilters("METHOD_MISSING_REQUIRED_ANNOTATIONS,CLASS_MISSING_REQUIRED_ANNOTATIONS")
                .build();

        WeaveViolationFilter filter = config.getWeaveViolationFilter();
        assertEquals("test", filter.getWeavePackage());
        assertTrue(filter.shouldIgnoreViolation(new WeaveViolation(WeaveViolationType.CLASS_MISSING_REQUIRED_ANNOTATIONS, "clazz")));
        assertTrue(filter.shouldIgnoreViolation(new WeaveViolation(WeaveViolationType.METHOD_MISSING_REQUIRED_ANNOTATIONS, "clazz")));
    }

    @Test
    public void ConfigBuilder_withoutConfiguredFilters_doesNotCreatesWeaveViolationFilter() {
        WeavePackageConfig.Builder builder = new WeavePackageConfig.Builder();
        WeavePackageConfig config = builder.name("test").build();

        assertNull(config.getWeaveViolationFilter());
    }

    @Test
    public void ConfigBuilder_clearReturnStacksDefault_defaultsToFalse() {
        WeavePackageConfig config = new WeavePackageConfig.Builder().name("test").build();

        assertFalse(config.isClearReturnStacksDefault());
    }

    @Test
    public void ConfigBuilder_withClearReturnStacksDefaultSetTrue_isClearReturnStacksDefaultReturnsTrue() {
        WeavePackageConfig config = new WeavePackageConfig.Builder().name("test")
                .clearReturnStacksDefault(true)
                .build();

        assertTrue(config.isClearReturnStacksDefault());
    }

    @Test
    public void ConfigBuilder_getName_returnsNameSetSoFar() {
        WeavePackageConfig.Builder builder = new WeavePackageConfig.Builder();
        assertNull(builder.getName());

        builder.name("test");
        assertEquals("test", builder.getName());
    }
}
