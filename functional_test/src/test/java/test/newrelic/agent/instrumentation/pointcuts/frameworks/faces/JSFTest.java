/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.agent.instrumentation.pointcuts.frameworks.faces;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.api.agent.Trace;
import com.sun.faces.lifecycle.Phase;
import com.sun.faces.mock.MockFacesContext;
import com.sun.faces.mock.MockLifecycle;
import org.junit.Assert;
import org.junit.Test;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class JSFTest {

    @Test
    public void testPhase() {
        final List<Tracer> tracers = new ArrayList<>();
        final MockFacesContext mockFacesContext = new MockFacesContext();
        final Phase phase = new Phase() {

            @Override
            public void execute(FacesContext arg0) throws FacesException {
                Tracer lastTracer = Transaction.getTransaction().getTransactionActivity().getLastTracer();
                tracers.add(lastTracer);
            }

            @Override
            protected void handleAfterPhase(FacesContext arg0, ListIterator<PhaseListener> arg1, PhaseEvent arg2) {
            }

            @Override
            protected void handleBeforePhase(FacesContext arg0, ListIterator<PhaseListener> arg1, PhaseEvent arg2) {
            }

            @Override
            protected void queueException(FacesContext ctx, Throwable t, String booleanKey) {
                super.queueException(ctx, t, booleanKey);
            }

            @Override
            protected void queueException(FacesContext ctx, Throwable t) {
                super.queueException(ctx, t);
            }

            @Override
            public PhaseId getId() {
                return PhaseId.ANY_PHASE;
            }
        };
        new Runnable() {
            @Override
            @Trace(dispatcher = true)
            public void run() {
                phase.doPhase(mockFacesContext, new MockLifecycle(), new ArrayList<PhaseListener>().listIterator());
            }
        }.run();

        Assert.assertEquals(1, tracers.size());
        Tracer tracer = tracers.get(0);

        ClassMethodSignature sig = new ClassMethodSignature(phase.getClass().getName(), "doPhase",
                "(Ljavax/faces/context/FacesContext;Ljavax/faces/lifecycle/Lifecycle;Ljava/util/ListIterator;)V");
        Assert.assertEquals(ClassMethodMetricNameFormat.getMetricName(sig, phase, MetricNames.CUSTOM),
                tracer.getMetricName());
    }
}
