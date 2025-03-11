/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akka22.test;

import akka.actor.ActorSystem;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.newrelic.test.marker.Java21IncompatibleTest;
import com.newrelic.test.marker.Java24IncompatibleTest;
import com.nr.instrumentation.akka22.test.actors.broadcasting.ActorA;
import com.nr.instrumentation.akka22.test.actors.broadcasting.ActorB;
import com.nr.instrumentation.akka22.test.actors.broadcasting.ActorC;
import com.nr.instrumentation.akka22.test.actors.broadcasting.branches.ActorNoTxnBranch;
import com.nr.instrumentation.akka22.test.actors.forwarding.ForwardActor;
import com.nr.instrumentation.akka22.test.actors.routing.Routee;
import com.nr.instrumentation.akka22.test.actors.routing.RoutingActor;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

// Not compatible with Java 11+ and Scala 2.13+ https://github.com/scala/bug/issues/12340
@Category({ Java17IncompatibleTest.class, Java21IncompatibleTest.class, Java24IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"akka.actor", "akka.dispatch", "akka.pattern", "akka.routing"})
public class AkkaTest {

    @Test
    public void testSend() throws InterruptedException {
        ActorSystem system = ActorSystem.create("AkkaTestSystem");
        TestApp.sendMessageInTransaction(system);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        assertEquals(1, finishedTransactionCount);
        String transactionName = "OtherTransaction/AkkaForward/Forward";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        assertTrue(metricsForTransaction.containsKey("Akka/forward/forwardActor"));
        assertTrue(metricsForTransaction.containsKey("Akka/receive/" + ForwardActor.class.getName()));
    }

    @Test
    public void testBroadcast() throws InterruptedException {
        ActorSystem system = ActorSystem.create("AkkaTestSystem");
        TestApp.broadcastInTransaction(system);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        assertEquals(1, finishedTransactionCount);
        String transactionName = "OtherTransaction/Akka/Broadcast";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        assertTrue(metricsForTransaction.containsKey("Akka/receive/" + ActorA.class.getName()));
        assertTrue(metricsForTransaction.containsKey("Akka/receive/" + ActorB.class.getName()));
        assertTrue(metricsForTransaction.containsKey("Akka/receive/" + ActorC.class.getName()));

        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertTrue(unscopedMetrics.containsKey("ActorA"));
        assertTrue(unscopedMetrics.containsKey("ActorB"));
        assertTrue(unscopedMetrics.containsKey("ActorC"));
    }

    @Test
    public void testBroadcastStress() throws InterruptedException {
        /**
         * @formatter:off
         *
         * This test tries to cover the following case:
         *
         * Suppose System broadcasts a message to A and B. B forwards the same message to C.
         *
         *             System
         *            /      \
         *          A         B @Trace(dispatcher = true)
         *                    |
         *                    C
         *
         *
         * A possible execution of this can be as follows:
         *
         *  A is busy, and does not read the message.
         *  B starts the transaction, and forwards message to C
         *  A wakes up, and reads message.
         *  C reads message.
         *
         *  We want to ensure that A never links to B.
         *
         *  To increase the likelihood of running into the execution above, we have actor A forward the same message to itself several times.
         *  We do the same thing for actor C.
         */

        ActorSystem system = ActorSystem.create("AkkaTestSystem");
        TestApp.broadcastStress(system);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        assertEquals(1, finishedTransactionCount);
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Akka/ParentActor"));

        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertFalse(unscopedMetrics.containsKey(ActorNoTxnBranch.ROLLUP_NAME));
    }

    @Test
    public void testRoutedActorSend() throws InterruptedException {
        ActorSystem system = ActorSystem.create("AkkaTestSystem");
        TestApp.sendRoutedMessageInTransaction(system);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        assertEquals(1, finishedTransactionCount);
        String transactionName = "OtherTransaction/Akka/Routing";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        assertTrue(metricsForTransaction.containsKey("Akka/receive/" + RoutingActor.class.getName()));
    }

    @Test
    public void testRoutedActorToRouteeSend() throws InterruptedException {
        ActorSystem system = ActorSystem.create("AkkaTestSystem");
        TestApp.sendRoutedMessageToRouteeInTransaction(system);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        assertEquals(1, finishedTransactionCount);
        String transactionName = "OtherTransaction/Akka/Routee";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        assertTrue(metricsForTransaction.containsKey("Akka/receive/" + RoutingActor.class.getName()));
        assertTrue(metricsForTransaction.containsKey("Akka/receive/" + Routee.class.getName()));
    }

}
