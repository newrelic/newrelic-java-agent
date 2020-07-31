/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.application;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.api.agent.ApplicationNamePriority;

public class PriorityApplicationNameTest {

    @Test
    public void create() {
        String expectedName = "MyApplicationName";
        ApplicationNamePriority expectedPriority = ApplicationNamePriority.FILTER_INIT_PARAM;
        PriorityApplicationName pan = PriorityApplicationName.create(expectedName, expectedPriority);
        Assert.assertEquals(expectedPriority, pan.getPriority());
        Assert.assertEquals(expectedName, pan.getName());
    }

    @Test
    public void createMultiple() {
        String appName = "MyApp1;MyApp2";
        ApplicationNamePriority expectedPriority = ApplicationNamePriority.FILTER_INIT_PARAM;
        PriorityApplicationName pan = PriorityApplicationName.create(appName, expectedPriority);
        Assert.assertEquals(expectedPriority, pan.getPriority());
        Assert.assertEquals("MyApp1", pan.getName());
        Assert.assertEquals(2, pan.getNames().size());
        Assert.assertEquals("MyApp1", pan.getNames().get(0));
        Assert.assertEquals("MyApp2", pan.getNames().get(1));
    }

    @Test
    public void equals() {
        String appName = "MyApp1;MyApp2";
        PriorityApplicationName pan = PriorityApplicationName.create(appName, ApplicationNamePriority.FILTER_INIT_PARAM);

        String appName2 = "MyApp1";
        PriorityApplicationName pan2 = PriorityApplicationName.create(appName2,
                ApplicationNamePriority.FILTER_INIT_PARAM);

        String appName3 = "MyApp2";
        PriorityApplicationName pan3 = PriorityApplicationName.create(appName3,
                ApplicationNamePriority.FILTER_INIT_PARAM);

        String appName4 = "MyApp2;MyApp1";
        PriorityApplicationName pan4 = PriorityApplicationName.create(appName4,
                ApplicationNamePriority.FILTER_INIT_PARAM);

        PriorityApplicationName pan5 = PriorityApplicationName.create(appName2,
                ApplicationNamePriority.SERVLET_INIT_PARAM);

        PriorityApplicationName pan6 = PriorityApplicationName.create(appName2,
                ApplicationNamePriority.SERVLET_INIT_PARAM);

        Assert.assertTrue(pan.equals(pan2));
        Assert.assertFalse(pan.equals(pan3));
        Assert.assertFalse(pan.equals(pan4));
        Assert.assertFalse(pan2.equals(pan5));
        Assert.assertTrue(pan5.equals(pan6));
    }

    @Test
    public void priority() {
        ApplicationNamePriority nonePriority = ApplicationNamePriority.NONE;
        ApplicationNamePriority contextPathPriority = ApplicationNamePriority.CONTEXT_PATH;
        ApplicationNamePriority contextNamePriority = ApplicationNamePriority.CONTEXT_NAME;
        ApplicationNamePriority contextParamPriority = ApplicationNamePriority.CONTEXT_PARAM;
        ApplicationNamePriority filterInitParamPriority = ApplicationNamePriority.FILTER_INIT_PARAM;
        ApplicationNamePriority servletInitParamPriority = ApplicationNamePriority.SERVLET_INIT_PARAM;
        ApplicationNamePriority attributePriority = ApplicationNamePriority.REQUEST_ATTRIBUTE;

        Assert.assertEquals(7, ApplicationNamePriority.values().length);
        Assert.assertTrue(nonePriority.compareTo(contextNamePriority) < 0);
        Assert.assertTrue(contextPathPriority.compareTo(contextNamePriority) < 0);
        Assert.assertTrue(contextNamePriority.compareTo(contextParamPriority) < 0);
        Assert.assertTrue(contextParamPriority.compareTo(filterInitParamPriority) < 0);
        Assert.assertTrue(filterInitParamPriority.compareTo(servletInitParamPriority) < 0);
        Assert.assertTrue(servletInitParamPriority.compareTo(attributePriority) < 0);
    }

}
