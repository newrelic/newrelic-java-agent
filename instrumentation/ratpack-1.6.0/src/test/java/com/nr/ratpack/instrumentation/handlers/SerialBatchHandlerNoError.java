package com.nr.ratpack.instrumentation.handlers;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import ratpack.exec.Promise;
import ratpack.exec.util.SerialBatch;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SerialBatchHandlerNoError implements Handler {
    @Override
    public void handle(Context ctx) {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Ratpack",
                "serialBatchNoError");
        List<Promise<String>> promisesBatch = new ArrayList<>();

        final AtomicInteger counter = new AtomicInteger();

        promisesBatch.add(Promise.async(downstream -> {
            NewRelic.addCustomParameter("SerialBatchHandlerNoError1", "true");
            counter.incrementAndGet();
            downstream.complete();
        }));

        promisesBatch.add(Promise.sync(() -> {
            NewRelic.addCustomParameter("SerialBatchHandlerNoError2", "true");
            counter.incrementAndGet();
            return "testing";
        }));

        SerialBatch.of(promisesBatch).yield().onError(e -> {
            ctx.error(new RuntimeException("Should not execute"));
        }).then(results -> {
            NewRelic.addCustomParameter("SerialBatchAllResults", "true");

            if (assertPromiseExecutionCount(counter.get(), promisesBatch.size())) {
                ctx.render("success");
            }
        });
    }

    private boolean assertPromiseExecutionCount(int count, int expected) {
        if (count != expected) {
            System.out.println("Error!");
            Thread.dumpStack();
            return false;
        }
        return true;
    }
}
