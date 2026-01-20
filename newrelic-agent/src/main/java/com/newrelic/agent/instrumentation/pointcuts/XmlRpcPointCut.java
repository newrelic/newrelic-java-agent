/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.InstrumentUtils;
import com.newrelic.agent.instrumentation.PointCut;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ExternalComponentTracer;
import com.newrelic.agent.tracers.IOTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class XmlRpcPointCut extends TracerFactoryPointCut {

    public XmlRpcPointCut(PointCutClassTransformer classTransformer) {
        super(XmlRpcPointCut.class, new InterfaceMatcher("javax/xml/rpc/Call"), createExactMethodMatcher("invoke",
                "([Ljava/lang/Object;)Ljava/lang/Object;",
                "(Ljavax/xml/namespace/QName;[Ljava/lang/Object;)Ljava/lang/Object;"));
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object call, Object[] args) {
        try {
            String endPoint = (String) call.getClass().getMethod("getTargetEndpointAddress").invoke(call);
            try {
                URL url = new URL(endPoint);
                String uri = InstrumentUtils.getURI(url);
                String methodName;
                if (sig == null) {
                    methodName = "";
                } else {
                    methodName = sig.getMethodName();
                }

                return new XmlRpcTracer(this, transaction, sig, call, url.getHost(), "XmlRpc", uri, new String[] { methodName });
            } catch (MalformedURLException e) {
                Agent.LOG.log(Level.FINE, "Unable to parse the target endpoint address for an XML RPC call", e);
            }
        } catch (Throwable e) {
            Agent.LOG.log(Level.FINE, "Unable to get the target endpoint address for an XML RPC call", e);
        }

        return null;
    }

    private static final class XmlRpcTracer extends ExternalComponentTracer implements IOTracer {
        private String uri;
        private String library;

        private XmlRpcTracer(PointCut pc, Transaction transaction, ClassMethodSignature sig, Object object,
                String host, String library, String uri, String[] operations) {
            super(transaction, sig, object, host, library, uri, operations);
            this.uri = uri;
            this.library = library;
        }

        private void finish() {
            try {
                NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                        .library(library)
                        .uri(new URI(uri))
                        .procedure("invoke")
                        .noInboundHeaders()
                        .build());
            } catch (URISyntaxException e) {
                NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                        .library(library)
                        .uri(URI.create("http://UnknownHost/"))
                        .procedure("invoke")
                        .noInboundHeaders()
                        .build());
            }
        }

        @Override
        public void finish(int opcode, Object returnValue) {
            finish();
            super.finish(opcode, returnValue);
        }

        @Override
        public void finish(Throwable throwable) {
            finish();
            super.finish(throwable);
        }
    }

}
