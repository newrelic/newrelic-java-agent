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
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.grpc", "com.nr.agent.instrumentation.grpc" })
public class BasicRequestsTest {

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
    public void testBlockingRequest() {
        client.helloBlocking("Blocking");

        String fullMethod = "helloworld.Greeter/SayHello";
        String clientTxName = "OtherTransaction/Custom/app.TestClient/helloBlocking";
        String serverTxName = "WebTransaction/gRPC/helloworld.Greeter/SayHello";
        ValidationHelper.validateGrpcInteraction(server, clientTxName, serverTxName, fullMethod, "UNARY", "Blocking");
    }

    @Test
    public void testFutureRequest() throws Exception {
        client.helloFuture("Future");

        String fullMethod = "helloworld.Greeter/SayHello";
        String clientTxName = "OtherTransaction/Custom/app.TestClient/helloFuture";
        String serverTxName = "WebTransaction/gRPC/helloworld.Greeter/SayHello";
        ValidationHelper.validateGrpcInteraction(server, clientTxName, serverTxName, fullMethod, "UNARY", "Future");
    }

    @Test
    public void testAsyncRequest() {
        client.helloAsync("Async");

        String fullMethod = "helloworld.Greeter/SayHello";
        String clientTxName = "OtherTransaction/Custom/app.TestClient/helloAsync";
        String serverTxName = "WebTransaction/gRPC/helloworld.Greeter/SayHello";
        ValidationHelper.validateGrpcInteraction(server, clientTxName, serverTxName, fullMethod, "UNARY", "Async");
    }

    @Test
    public void testStreamingRequest() {
        client.helloStreaming("Streaming");

        String fullMethod = "manualflowcontrol.StreamingGreeter/SayHelloStreaming";
        String clientTxName = "OtherTransaction/Custom/app.TestClient/helloStreaming";
        String serverTxName = "WebTransaction/gRPC/" + fullMethod;
        ValidationHelper.validateGrpcInteraction(server, clientTxName, serverTxName, fullMethod, "BIDI_STREAMING", "Streaming");
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
        assertEquals(2, errorEvents.size());
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
        assertEquals(1, errorEvents.size());
    }

}
