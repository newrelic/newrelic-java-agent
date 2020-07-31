/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

import app.TestClient;
import app.TestServer;
import com.newrelic.agent.introspec.CatHelper;
import com.newrelic.agent.introspec.ErrorEvent;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.grpc", "com.nr.agent.instrumentation.grpc" }, configName = "no_grpc_errors.yml")
public class GrpcErrorsTest {

    private static TestServer server;
    private static TestClient client;

    @BeforeClass
    public static void before() throws Exception {
        server = new TestServer();
        server.start();
        client = new TestClient("localhost", server.getPort());
    }

    @AfterClass
    public static void after() throws InterruptedException {
        if (client != null) {
            client.shutdown();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testUncaughtException() {
        try {
            client.throwException("Blocking");
        } catch (Exception e) {
        }

        String fullMethod = "helloworld.Greeter/ThrowException";
        String clientTxName = "OtherTransaction/Custom/app.TestClient/throwException";
        String serverTxName = "WebTransaction/gRPC/helloworld.Greeter/ThrowException";
        ValidationHelper.validateExceptionGrpcInteraction(server, clientTxName, serverTxName, fullMethod, "UNARY", "Blocking", 2);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<ErrorEvent> errorEvents = introspector.getErrorEvents();
        CatHelper.verifyOneSuccessfulCat(introspector, clientTxName, serverTxName);

        // Even though gRPC errors are disabled this will still generate an error event
        // because the uncaught exception triggers the agent's built-in error capture
        assertEquals(1, errorEvents.size());
    }

    @Test
    public void testCaughtException() {
        try {
            client.throwCaughtException("Blocking");
        } catch (Exception e) {
        }

        String fullMethod = "helloworld.Greeter/ThrowCaughtException";
        String clientTxName = "OtherTransaction/Custom/app.TestClient/throwCaughtException";
        String serverTxName = "WebTransaction/gRPC/helloworld.Greeter/ThrowCaughtException";
        ValidationHelper.validateExceptionGrpcInteraction(server, clientTxName, serverTxName, fullMethod, "UNARY", "Blocking", 10);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<ErrorEvent> errorEvents = introspector.getErrorEvents();
        CatHelper.verifyOneSuccessfulCat(introspector, clientTxName, serverTxName);

        // Disabling the gRPC error capture prevents error events from being created
        assertEquals(0, errorEvents.size());
    }

}
