/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.junit.Assert;

import org.junit.Test;

import com.newrelic.agent.service.ServiceFactory;

public class ClassNameFilterFuncTest {
    @Test
    public void phase() {
        // ^(com/sun/faces/|javax/faces/)(.*)|(^sun/net/www/protocol/http/HttpURLConnection\$HttpInputStream$)|(^org/apache/commons/dbcp/DelegatingStatement$)|(^java/net/HttpURLConnection$)|(^com/sun/jersey/impl/model/method/dispatch/ResourceJavaMethodDispatcher$)|(^java/lang/UNIXProcess$)|(^com/sun/faces/lifecycle/Phase$)|(^com/sun/faces/mock/MockLifecycle$)|(^com/sun/jersey/server/impl/model/method/dispatch/ResourceJavaMethodDispatcher$)|com/newrelic/.*Test|(^com/sun/faces/lifecycle/LifecycleImpl$)|(^java/util/concurrent/Executors\$RunnableAdapter$)|(^java/net/SocketInputStream)|(^java/lang/ProcessImpl$)|(^java/net/SocketInputStream$)|(^java/lang/(ProcessImpl|UNIXProcess))|(^sun/net/www/protocol/http/HttpURLConnection\$HttpInputStream)
        PointCutClassTransformer classTransformer = ServiceFactory.getClassTransformerService().getClassTransformer();
        Assert.assertTrue(classTransformer.getClassNameFilter().isIncluded("com/sun/faces/lifecycle/Phase"));
    }

    @Test
    public void lifecycleImpl() {
        PointCutClassTransformer classTransformer = ServiceFactory.getClassTransformerService().getClassTransformer();
        Assert.assertTrue(classTransformer.getClassNameFilter().isIncluded("com/sun/faces/lifecycle/LifecycleImpl"));
    }

}
