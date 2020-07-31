/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Config2Test {

    private Map<String, Object> createMap() {
        Map<String, Object> configMap = new HashMap<>();
        return configMap;
    }

    @Test
    public void getUniqueStrings() {
        String[] strings = BaseConfig.getUniqueStringsFromString("z;z;a;b", ";").toArray(new String[0]);
        Assert.assertEquals(3, strings.length);
        Assert.assertEquals("z", strings[0]);
        Assert.assertEquals("a", strings[1]);
        Assert.assertEquals("b", strings[2]);
    }

    @Test
    public void getIntegerSetFromCollectionOfIntegers() throws Exception {
        Map<String, Object> settings = createMap();
        Set<Integer> setOfIntegers = new HashSet<>();
        setOfIntegers.add(403);
        setOfIntegers.add(404);
        settings.put("setOfIntegers", setOfIntegers);

        BaseConfig config = new BaseConfig(settings);
        Set<Integer> actual = config.getIntegerSet("setOfIntegers", null);

        Assert.assertEquals(2, actual.size());
        Assert.assertTrue(actual.containsAll(setOfIntegers));
    }

    @Test
    public void getIntegerSetFromCollectionOfLongs() throws Exception {
        Map<String, Object> settings = createMap();
        Set<Long> setOfLongs = new HashSet<>();
        setOfLongs.add(403L);
        setOfLongs.add(404L);
        settings.put("setOfLongs", setOfLongs);

        BaseConfig config = new BaseConfig(settings);
        Set<Integer> actual = config.getIntegerSet("setOfLongs", null);

        Assert.assertEquals(2, actual.size());
        Assert.assertTrue(actual.contains(403));
        Assert.assertTrue(actual.contains(404));
    }

    @Test
    public void getIntegerSetFromString() throws Exception {
        Map<String, Object> settings = createMap();
        String integerString = "403,404, 405,,406";
        settings.put("integerString", integerString);

        BaseConfig config = new BaseConfig(settings);
        Set<Integer> actual = config.getIntegerSet("integerString", null);

        Assert.assertEquals(4, actual.size());
        Assert.assertTrue(actual.contains(403));
        Assert.assertTrue(actual.contains(404));
        Assert.assertTrue(actual.contains(405));
        Assert.assertTrue(actual.contains(406));
    }

    @Test
    public void getIntegerSetNull() throws Exception {
        Map<String, Object> settings = createMap();

        BaseConfig config = new BaseConfig(settings);
        Set<Integer> actual = config.getIntegerSet("doesNotExist", null);

        Assert.assertNull(actual);
    }

    @Test
    public void getIntegerSetDefaultValue() throws Exception {
        Map<String, Object> settings = createMap();

        BaseConfig config = new BaseConfig(settings);
        Set<Integer> expected = new HashSet<>();
        expected.add(403);
        Set<Integer> actual = config.getIntegerSet("doesNotExist", expected);

        Assert.assertEquals(1, actual.size());
        Assert.assertTrue(actual.contains(403));
    }

}
