/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.play2816;

import org.junit.rules.ExternalResource;

import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.Helpers;
import play.test.TestServer;

import static play.inject.Bindings.bind;

public class PlayApplicationServerRule extends ExternalResource {

    private Application application;
    private TestServer testServer;
    private final int serverPort;

    public PlayApplicationServerRule(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    protected void before() throws Throwable {
        application = new GuiceApplicationBuilder()
                .bindings(
                    bind(SimpleJavaController.class).toSelf().eagerly(),
                    bind(SimpleScalaController.class).toSelf().eagerly(),
                    bind(SimpleJavaAction.class).toSelf().eagerly())
                .build();

        testServer = Helpers.testServer(serverPort, application);
        testServer.start();
    }

    @Override
    protected void after() {
        testServer.stop();
    }

    public Application getApplication() {
        return application;
    }

    public TestServer getTestServer() {
        return testServer;
    }
}
