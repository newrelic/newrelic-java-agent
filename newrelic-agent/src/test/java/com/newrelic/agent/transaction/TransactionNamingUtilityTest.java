/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.bridge.TransactionNamePriority;
import org.junit.Assert;
import org.junit.Test;

public class TransactionNamingUtilityTest {
    @Test
    public void isGreaterThan() {
        Assert.assertTrue(TransactionNamingUtility.isGreaterThan(TransactionNamePriority.FRAMEWORK, TransactionNamePriority.FILTER_NAME, TransactionNamingScheme.LEGACY));

        Assert.assertTrue(TransactionNamingUtility.isGreaterThan(TransactionNamePriority.FILTER_INIT_PARAM, TransactionNamePriority.REQUEST_URI, TransactionNamingScheme.LEGACY));
        Assert.assertFalse(TransactionNamingUtility.isGreaterThan(TransactionNamePriority.FRAMEWORK, TransactionNamePriority.FRAMEWORK, TransactionNamingScheme.LEGACY));
    }

    @Test
    public void isLessThan() {
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FRAMEWORK, TransactionNamePriority.CUSTOM_HIGH, TransactionNamingScheme.LEGACY));

        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FILTER_INIT_PARAM, TransactionNamePriority.SERVLET_INIT_PARAM,
                TransactionNamingScheme.LEGACY));
        Assert.assertFalse(TransactionNamingUtility.isLessThan(TransactionNamePriority.FRAMEWORK, TransactionNamePriority.FRAMEWORK, TransactionNamingScheme.LEGACY));
    }

    @Test
    public void legacyPriority() {
        Assert.assertEquals(14, TransactionNamePriority.values().length);
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.NONE, TransactionNamePriority.REQUEST_URI, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.REQUEST_URI, TransactionNamePriority.STATUS_CODE, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.STATUS_CODE, TransactionNamePriority.FILTER_NAME, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FILTER_NAME, TransactionNamePriority.FILTER_INIT_PARAM, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FILTER_INIT_PARAM, TransactionNamePriority.SERVLET_NAME, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.SERVLET_NAME, TransactionNamePriority.SERVLET_INIT_PARAM, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.SERVLET_INIT_PARAM, TransactionNamePriority.JSP, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.JSP, TransactionNamePriority.FRAMEWORK_LOW, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FRAMEWORK_LOW, TransactionNamePriority.FRAMEWORK, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FRAMEWORK, TransactionNamePriority.FRAMEWORK_HIGH, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FRAMEWORK_HIGH, TransactionNamePriority.CUSTOM_HIGH, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.CUSTOM_HIGH, TransactionNamePriority.FROZEN, TransactionNamingScheme.LEGACY));
    }

    @Test
    public void uriPriority() {
        Assert.assertEquals(14, TransactionNamePriority.values().length);
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.NONE, TransactionNamePriority.FILTER_NAME, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FILTER_NAME, TransactionNamePriority.SERVLET_NAME, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.SERVLET_NAME, TransactionNamePriority.JSP, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.JSP, TransactionNamePriority.REQUEST_URI, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.REQUEST_URI, TransactionNamePriority.STATUS_CODE, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.STATUS_CODE, TransactionNamePriority.FILTER_INIT_PARAM, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FILTER_INIT_PARAM, TransactionNamePriority.SERVLET_INIT_PARAM, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.SERVLET_INIT_PARAM, TransactionNamePriority.FRAMEWORK_LOW, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FRAMEWORK_LOW, TransactionNamePriority.FRAMEWORK, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FRAMEWORK, TransactionNamePriority.FRAMEWORK_HIGH, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FRAMEWORK_HIGH, TransactionNamePriority.CUSTOM_HIGH, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.CUSTOM_HIGH, TransactionNamePriority.FROZEN, TransactionNamingScheme.RESOURCE_BASED));
    }


    @Test
    public void equals() {
        PriorityTransactionName ptn = PriorityTransactionName.create("MyTransactionName", null,
                TransactionNamePriority.FILTER_INIT_PARAM);
        Assert.assertTrue(ptn.equals(ptn));

        PriorityTransactionName ptn2 = PriorityTransactionName.create("MyTransactionName", null,
                TransactionNamePriority.FILTER_NAME);
        Assert.assertFalse(ptn.equals(ptn2));

        PriorityTransactionName ptn3 = PriorityTransactionName.create("MyOtherTransactionName", null,
                TransactionNamePriority.FILTER_INIT_PARAM);
        Assert.assertFalse(ptn.equals(ptn3));

        Assert.assertFalse(ptn.equals(null));
        Assert.assertFalse(ptn.equals(TransactionNamePriority.NONE));
        Assert.assertFalse(PriorityTransactionName.NONE.equals(ptn));
        Assert.assertTrue(PriorityTransactionName.NONE.equals(PriorityTransactionName.NONE));
    }

    @Test
    public void uriGreaterThanServlet() {
        // URI name should override servlet and filter names when using URI-based naming
        Assert.assertTrue(TransactionNamingUtility.isGreaterThan(TransactionNamePriority.REQUEST_URI, TransactionNamePriority.SERVLET_NAME, TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isGreaterThan(TransactionNamePriority.REQUEST_URI, TransactionNamePriority.FILTER_NAME, TransactionNamingScheme.RESOURCE_BASED));

        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.SERVLET_NAME, TransactionNamePriority.REQUEST_URI,
                TransactionNamingScheme.RESOURCE_BASED));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.FILTER_NAME, TransactionNamePriority.REQUEST_URI,
                TransactionNamingScheme.RESOURCE_BASED));


        // Servlet name  and filter name win over URI in legacy mode
        Assert.assertTrue(TransactionNamingUtility.isGreaterThan(TransactionNamePriority.SERVLET_NAME, TransactionNamePriority.REQUEST_URI, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isGreaterThan(TransactionNamePriority.FILTER_NAME, TransactionNamePriority.REQUEST_URI, TransactionNamingScheme.LEGACY));

        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.REQUEST_URI, TransactionNamePriority.SERVLET_NAME, TransactionNamingScheme.LEGACY));
        Assert.assertTrue(TransactionNamingUtility.isLessThan(TransactionNamePriority.REQUEST_URI, TransactionNamePriority.FILTER_NAME, TransactionNamingScheme.LEGACY));
    }
}
