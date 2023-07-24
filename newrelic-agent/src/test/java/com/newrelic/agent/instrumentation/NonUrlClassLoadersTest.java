package com.newrelic.agent.instrumentation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NonUrlClassLoadersTest {
    @Test
    public void getNonUrlType_withNullName_returnsNull() {
        assertNull(NonUrlClassLoaders.getNonUrlType(null));
    }

    @Test
    public void getNonUrlType_withValidName_returnsProperInstance() {
        assertEquals(NonUrlClassLoaders.JBOSS_6, NonUrlClassLoaders.getNonUrlType("org.jboss.classloader.spi.base.BaseClassLoader"));
    }

    @Test
    public void getNonUrlType_withInvalidName_returnsNull() {
        assertNull(NonUrlClassLoaders.getNonUrlType("foo"));
    }
}
