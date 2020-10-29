/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package app;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.NewRelic;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.manualflowcontrol.StreamingGreeterGrpc;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

public class TestServer {
    private Server server;

    private int port;

    public void start() throws IOException {
        port = InstrumentationTestRunner.getIntrospector().getRandomPort();
        server = ServerBuilder.forPort(port)
                .addService(new GreeterImpl())
                .addService(new StreamingGreeterImpl())
                .intercept(new NewRelicServerInterceptor())
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                TestServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public int getPort() {
        return port;
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            try {
                NewRelic.addCustomParameter("sayHelloBefore", req.getName());
                HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
                NewRelic.addCustomParameter("sayHelloAfter", req.getName());
            }
            catch (Exception e){
                responseObserver.onError(Status.ABORTED.asException());
            }
        }

        @Override
        public void throwException(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            int myNum = 7/0;
        }

        @Override
        public void throwCaughtException(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            try {
                int myNum = 7/0;
            }
            catch (Exception e){
                responseObserver.onError(Status.ABORTED.asException());
            }
        }
    }

    static class StreamingGreeterImpl extends StreamingGreeterGrpc.StreamingGreeterImplBase {

        @Override
        public StreamObserver<io.grpc.examples.manualflowcontrol.HelloRequest> sayHelloStreaming(
                final StreamObserver<io.grpc.examples.manualflowcontrol.HelloReply> responseObserver) {
            NewRelic.addCustomParameter("customParameter", "true");
            return new StreamObserver<io.grpc.examples.manualflowcontrol.HelloRequest>() {
                @Override
                public void onNext(io.grpc.examples.manualflowcontrol.HelloRequest value) {
                    try {
                        io.grpc.examples.manualflowcontrol.HelloReply reply = io.grpc.examples.manualflowcontrol.HelloReply
                                .newBuilder()
                                .setMessage("Hello " + value.getName())
                                .build();
                        responseObserver.onNext(reply);
                        responseObserver.onCompleted();
                    } catch (Exception e){
                        responseObserver.onError(Status.ABORTED.asException());
                    }
                }

                @Override
                public void onError(Throwable t) {

                }

                @Override
                public void onCompleted() {

                }
            };
        }

    }
}
