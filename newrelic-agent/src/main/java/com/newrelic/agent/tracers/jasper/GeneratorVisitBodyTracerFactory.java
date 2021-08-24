/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.MethodExitTracerNoSkip;
import com.newrelic.agent.tracers.Tracer;

public class GeneratorVisitBodyTracerFactory extends AbstractTracerFactory {

    public GeneratorVisitBodyTracerFactory() {
        super();
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, final Object visitorObj, Object[] args) {
        if (!GeneratorVisitTracerFactory.isAutoInstrumentationEnabled()) {
            return null;
        }

        try {
            // check if page has been excluded
            String page = GeneratorVisitTracerFactory.getPage(transaction);
            if (GeneratorVisitTracerFactory.isIgnorePageForAutoInstrument(page, transaction)) {
                return null;
            }

            JasperClassFactory factory = JasperClassFactory.getJasperClassFactory(visitorObj.getClass().getClassLoader());
            Node node = factory.getNode(args[0]);
            final Visitor visitor = factory.getVisitor(visitorObj);
            String nodeName = node.getQName();

            if ("head".equalsIgnoreCase(nodeName) && checkParentNode(node, "html")) {
                Agent.LOG.fine("Compiling the browser timing header into a jsp");
                visitor.writeScriptlet(AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET);
            } else if ("body".equalsIgnoreCase(nodeName) && checkParentNode(node, "html")) {
                return new MethodExitTracerNoSkip(sig, transaction) {

                    @Override
                    public String getGuid() {
                        return null;
                    }

                    @Override
                    protected void doFinish(int opcode, Object returnValue) {
                        writeFooter(visitor);
                    }

                };
            }

        } catch (Exception e) {
            Agent.LOG.log(Level.FINE, "An error occurred auto enabling real user monitoring", e);
        }
        return null;
    }

    private void writeFooter(Visitor visitor) {
        Agent.LOG.fine("Compiling the browser timing footer into a jsp");
        try {
            visitor.writeScriptlet(AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET);
        } catch (Exception ex) {
            Agent.LOG.log(Level.FINE, "An error occurred auto enabling real user monitoring", ex);
        }
    }

    private boolean checkParentNode(Node node, String name) throws Exception {

        Node parent = node.getParent();
        if (parent != null) {
            return name.equals(parent.getQName());
        }
        return false;
    }

}