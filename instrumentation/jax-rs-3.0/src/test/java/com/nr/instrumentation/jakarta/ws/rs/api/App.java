/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jakarta.ws.rs.api;

import com.newrelic.api.agent.Trace;

public final class App {
    @Trace(dispatcher = true)
    public static String callUserServiceImpl() {
        UserServiceImpl service = new UserServiceImpl();
        return service.getUserFeatures();
    }

    @Trace(dispatcher = true)
    public static String callInterfaceResourceInTransaction() {
        InterfaceResourceImpl impl = new InterfaceResourceImpl();
        return impl.getIt();
    }

    @Trace(dispatcher = true)
    public static String callInterfaceWithImplResourceInTransaction() {
        PeopleInterfaceResource impl = new PeopleResource();
        return impl.getPeople();
    }

    @Trace(dispatcher = true)
    public static String callClassResourceInTransaction() {
        ClassResource classResource = new ClassResource();
        return classResource.getIt();
    }

    @Trace(dispatcher = true)
    public static String callClassResourcePatchInTransaction() {
        ClassResource classResource = new ClassResource();
        return classResource.patchIt();
    }

    @Trace(dispatcher = true)
    public static String callClassResourcePutInTransaction() {
        ClassResource classResource = new ClassResource();
        return classResource.putIt();
    }
    @Trace(dispatcher = true)
    public static String callClassResourcePostInTransaction() {
        ClassResource classResource = new ClassResource();
        return classResource.postIt();
    }
    @Trace(dispatcher = true)
    public static String callExceptionInterfaceResourceInTransaction() {
        InterfaceResourceImpl impl = new InterfaceResourceImpl();
        return impl.exceptionTest();
    }

    @Trace(dispatcher = true)
    public static String callExceptionClassResourceInTransaction() {
        ClassResource classResource = new ClassResource();
        return classResource.exceptionTest();
    }

    @Trace(dispatcher = true)
    public static String callLargeParametersClassResourceInTransaction() {
        ClassResource classResource = new ClassResource();
        return classResource.largeNumberOfParametersTest("one", "two", 3, 4, 5, "six", new Object(), 8L, 9, 10);
    }

    @Trace(dispatcher = true)
    public static String callStaticEndpoint() {
        StaticEndpoint.staticMethod();
        return "static endpoint was called";
    }

    @Trace(dispatcher = true)
    public static String callInnerClass() {
        StaticEndpoint.InnerClass.innerStatic();
        return "inner class endpoint was called";
    }
}
