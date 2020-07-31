/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.Mocks;

/* (non-javadoc)
 * Note: the "beacon" was a predecessor technology for correlated transaction traces with the browser. 
 * Some appearances of the term could be changed to "browser" now.
 */

public class BrowserMonitoringConfigImplTest {

    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void isAutoInstrument() throws Exception {
        Map<String, Object> beaconMap = ImmutableMap.<String, Object> of(BrowserMonitoringConfigImpl.AUTO_INSTRUMENT,
                !BrowserMonitoringConfigImpl.DEFAULT_AUTO_INSTRUMENT);
        BrowserMonitoringConfig config = BrowserMonitoringConfigImpl.createBrowserMonitoringConfig(beaconMap);

        Assert.assertEquals(!BrowserMonitoringConfigImpl.DEFAULT_AUTO_INSTRUMENT, config.isAutoInstrumentEnabled());
    }

    @Test
    public void isAutoInstrumentSystemProperty() throws Exception {
        String key = BrowserMonitoringConfigImpl.SYSTEM_PROPERTY_ROOT + BrowserMonitoringConfigImpl.AUTO_INSTRUMENT;
        String val = String.valueOf(!BrowserMonitoringConfigImpl.DEFAULT_AUTO_INSTRUMENT);

        Mocks.createSystemPropertyProvider(ImmutableMap.of(key, val));

        Map<String, Object> beaconMap = ImmutableMap.<String, Object> of(BrowserMonitoringConfigImpl.AUTO_INSTRUMENT,
                BrowserMonitoringConfigImpl.DEFAULT_AUTO_INSTRUMENT);
        BrowserMonitoringConfig config = BrowserMonitoringConfigImpl.createBrowserMonitoringConfig(beaconMap);

        Assert.assertEquals(!BrowserMonitoringConfigImpl.DEFAULT_AUTO_INSTRUMENT, config.isAutoInstrumentEnabled());
    }

    @Test
    public void isAutoInstrumentDefault() throws Exception {
        Map<String, Object> beaconMap = ImmutableMap.of();
        BrowserMonitoringConfig config = BrowserMonitoringConfigImpl.createBrowserMonitoringConfig(beaconMap);

        Assert.assertEquals(BrowserMonitoringConfigImpl.DEFAULT_AUTO_INSTRUMENT, config.isAutoInstrumentEnabled());
    }

    @Test
    public void isDebugDefault() throws Exception {
        Map<String, Object> beaconMap = ImmutableMap.of();
        BrowserMonitoringConfig config = BrowserMonitoringConfigImpl.createBrowserMonitoringConfig(beaconMap);

        Assert.assertEquals(BrowserMonitoringConfigImpl.DEFAULT_DEBUG, config.isDebug());
    }

    @Test
    public void isDebugSystemProperty() throws Exception {
        String key = BrowserMonitoringConfigImpl.SYSTEM_PROPERTY_ROOT + BrowserMonitoringConfigImpl.DEBUG;
        String val = String.valueOf(!BrowserMonitoringConfigImpl.DEFAULT_DEBUG);

        Mocks.createSystemPropertyProvider(ImmutableMap.of(key, val));

        Map<String, Object> beaconMap = ImmutableMap.<String, Object> of(BrowserMonitoringConfigImpl.DEBUG,
                BrowserMonitoringConfigImpl.DEFAULT_DEBUG);
        BrowserMonitoringConfig config = BrowserMonitoringConfigImpl.createBrowserMonitoringConfig(beaconMap);

        Assert.assertEquals(!BrowserMonitoringConfigImpl.DEFAULT_DEBUG, config.isDebug());
    }

    @Test
    public void isLoaderTypeDefault() throws Exception {
        Map<String, Object> beaconMap = ImmutableMap.of();
        BrowserMonitoringConfig config = BrowserMonitoringConfigImpl.createBrowserMonitoringConfig(beaconMap);

        Assert.assertEquals(BrowserMonitoringConfigImpl.DEFAULT_LOADER_TYPE, config.getLoaderType());
    }

    @Test
    public void isLoaderTypeSystemProperty() throws Exception {
        String key = BrowserMonitoringConfigImpl.SYSTEM_PROPERTY_ROOT + BrowserMonitoringConfigImpl.LOADER_TYPE;
        String val = String.valueOf("none");

        Mocks.createSystemPropertyProvider(ImmutableMap.of(key, val));

        Map<String, Object> beaconMap = ImmutableMap.<String, Object> of(BrowserMonitoringConfigImpl.LOADER_TYPE,
                BrowserMonitoringConfigImpl.DEFAULT_LOADER_TYPE);
        BrowserMonitoringConfig config = BrowserMonitoringConfigImpl.createBrowserMonitoringConfig(beaconMap);

        Assert.assertEquals("none", config.getLoaderType());
    }

}
