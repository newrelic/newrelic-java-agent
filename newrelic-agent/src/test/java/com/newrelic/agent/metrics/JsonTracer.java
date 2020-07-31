/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.objectweb.asm.Opcodes;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;

public class JsonTracer {
    private String asyncUnit;
    private String tracerName;
    private Long startTimeNs;
    private Long durationNs;
    private TracerType tracerType;
    private final List<RegisterContext> registerAsyncContext = new ArrayList<>();
    private Object startAsyncContext;
    private boolean lastByteEndOfTxa;
    private Long lastByteTimeNs;
    private Long firstByteTimeNs;
    private final boolean alreadyFinished = false;

    private DefaultTracer me;

    public static JsonTracer createJsonTracer(JSONObject tracerObject, Map<Object, ContextObject> contexts) {
        JsonTracer tracer = new JsonTracer();

        Object obj = tracerObject.get("async_unit");
        tracer.asyncUnit = (obj == null) ? null : (String) obj;

        obj = tracerObject.get("name");
        tracer.tracerName = (obj == null) ? null : (String) obj;

        obj = tracerObject.get("start");
        tracer.startTimeNs = (obj == null) ? null : TimeUnit.NANOSECONDS.convert((Long) obj, TimeUnit.MILLISECONDS);

        obj = tracerObject.get("duration");
        tracer.durationNs = (obj == null) ? null : TimeUnit.NANOSECONDS.convert((Long) obj, TimeUnit.MILLISECONDS);

        obj = tracerObject.get("tracer_type");
        tracer.tracerType = (obj == null) ? TracerType.DEFAULT : (TracerType.valueOf((String) obj));

        obj = tracerObject.get("register_async");
        setRegisterAsync(tracer, obj, contexts);

        obj = tracerObject.get("start_async");
        if (obj != null) {
            tracer.startAsyncContext = getContextObject(obj, contexts);
        }

        obj = tracerObject.get("lastByteOnTxa");
        tracer.lastByteEndOfTxa = (obj == null) ? false : (Boolean) obj;

        obj = tracerObject.get("lastByte");
        tracer.lastByteTimeNs = (obj == null) ? null : TimeUnit.NANOSECONDS.convert((Long) obj, TimeUnit.MILLISECONDS);

        obj = tracerObject.get("firstByte");
        tracer.firstByteTimeNs = (obj == null) ? null : TimeUnit.NANOSECONDS.convert((Long) obj, TimeUnit.MILLISECONDS);

        validate(tracer);

        return tracer;
    }

    private static void setRegisterAsync(JsonTracer tracer, Object obj, Map<Object, ContextObject> contexts) {
        if (obj != null) {
            JSONArray registers = (JSONArray) obj;
            for (Object current : registers) {
                if (current instanceof JSONArray) {
                    JSONArray array = (JSONArray) current;
                    tracer.registerAsyncContext.add(getContextObjectAndCreateRegister(array, contexts));
                } else {
                    tracer.registerAsyncContext.add(getContextObjectAndCreateRegister(registers, contexts));
                    break;
                }
            }
        }
    }

    private static ContextObject getContextObject(Object obj, Map<Object, ContextObject> contexts) {

        ContextObject cont = contexts.get(obj);
        if (cont == null) {
            cont = new ContextObject(obj);
            contexts.put(obj, cont);
        }
        return cont;
    }

    private static RegisterContext getContextObjectAndCreateRegister(JSONArray obj, Map<Object, ContextObject> contexts) {

        Object contextInput = obj.get(0);
        ContextObject cont = contexts.get(contextInput);
        if (cont == null) {
            cont = new ContextObject(contextInput);
            contexts.put(contextInput, cont);
        }
        return new RegisterContext(cont, (Long) obj.get(1));
    }

    private static void validate(JsonTracer tracer) {
        if (tracer.asyncUnit == null) {
            throw new IllegalArgumentException("The async_unit must be set on in the tracer json.");
        } else if (tracer.tracerName == null) {
            throw new IllegalArgumentException("The name must be set on in the tracer json.");
        } else if (tracer.startTimeNs == null) {
            throw new IllegalArgumentException("The start must be set on in the tracer json.");
        } else if (tracer.durationNs == null) {
            throw new IllegalArgumentException("The duration must be set on in the tracer json.");
        }
    }

    public DefaultTracer generateTracer(Long txaStartTime, boolean firstThreadActivity) {
        if (txaStartTime == null || firstThreadActivity) {
            if (tracerType == TracerType.WEB) {
                me = generateRootWebTracer(txaStartTime);
            } else {
                me = generateRootBackgroundTracer(txaStartTime);
            }
        } else {
            me = generateDefaultTracer(txaStartTime);
        }

        if (startAsyncContext != null) {
            ServiceFactory.getAsyncTxService().startAsyncActivity(startAsyncContext);
        }

        for (RegisterContext curr : registerAsyncContext) {
            ServiceFactory.getAsyncTxService().registerAsyncActivity(curr.getContext());
        }

        return me;
    }

    private DefaultTracer generateDefaultTracer(long txaStartTime) {
        ClassMethodSignature sig = new ClassMethodSignature("clazz", tracerName, "()");
        Object reference = new Object();
        DefaultTracer tracer = new DefaultTracer(Transaction.getTransaction().getTransactionActivity(), sig, reference,
                new ClassMethodMetricNameFormat(sig, reference), txaStartTime + startTimeNs);
        Transaction.getTransaction().getTransactionActivity().tracerStarted(tracer);
        return tracer;
    }

    private DefaultTracer generateRootBackgroundTracer(Long initTime) {
        long time = getStartTime(initTime);
        ClassMethodSignature sig = new ClassMethodSignature("clazz", tracerName, "()");
        Object reference = new Object();

        int flags = TracerFlags.DISPATCHER;
        if (startTimeNs != 0) {
            flags = TracerFlags.ASYNC;
        }

        DefaultTracer tracer = new OtherRootTracer(Transaction.getTransaction().getTransactionActivity(), sig,
                reference, new ClassMethodMetricNameFormat(sig, reference), flags, time);
        Transaction.getTransaction().getTransactionActivity().tracerStarted(tracer);
        return tracer;
    }

    private DefaultTracer generateRootWebTracer(Long initTime) {
        DefaultTracer tracer = generateRootBackgroundTracer(initTime);
        Transaction.getTransaction().setWebRequest(new MockHttpRequest());
        Transaction.getTransaction().setWebResponse(new MockHttpResponse());
        return tracer;
    }

    private long getStartTime(Long initTime) {
        long time;
        if (initTime == null) {
            time = System.nanoTime();
        } else {
            time = initTime + startTimeNs;
        }
        return time;
    }

    public void endTx(long pStartTime) throws Exception {
        if (!alreadyFinished && firstByteTimeNs != null) {
            me.getTransactionActivity().getTransaction().markFirstByteOfResponse(pStartTime + firstByteTimeNs);
        }
        if (!alreadyFinished && lastByteTimeNs != null) {
            me.getTransactionActivity().getTransaction().markLastByteOfResponse(pStartTime + lastByteTimeNs);
        }
    }

    public void finishTracer(long pStartTime) throws Exception {
        if (!alreadyFinished) {
            if (lastByteEndOfTxa) {
                me.getTransactionActivity().getTransaction().addOutboundResponseHeaders();
            }

            if (firstByteTimeNs != null) {
                me.getTransactionActivity().getTransaction().markFirstByteOfResponse(pStartTime + firstByteTimeNs);
            }
            if (lastByteTimeNs != null) {
                me.getTransactionActivity().getTransaction().markLastByteOfResponse(pStartTime + lastByteTimeNs);
            }
        }
        me.performFinishWork(pStartTime + startTimeNs + durationNs, Opcodes.RETURN, null);
    }

    public String getAsyncUnit() {
        return asyncUnit;
    }

    public String getTracerName() {
        return tracerName;
    }

    public Long getStartTime() {
        return startTimeNs;
    }

    public Long getDuration() {
        return durationNs;
    }

    public TracerType getTracerType() {
        return tracerType;
    }

    public List<RegisterContext> getRegisterAsyncContexts() {
        return registerAsyncContext;
    }

    public Object getStartAsyncContext() {
        return startAsyncContext;
    }

    public static enum TracerType {
        WEB, BACKGROUND, DEFAULT;
    }

    public Long getEndTime() {
        return lastByteTimeNs;
    }
}
