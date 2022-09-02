package com.nr.ratpack.instrumentation.handlers;

import com.newrelic.api.agent.NewRelic;
import ratpack.exec.Promise;
import ratpack.exec.util.ParallelBatch;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelBatchForEach implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        List<Promise<String>> promisesBatch = new ArrayList<>();
        promisesBatch.add(Promise.async(downstream -> {
            NewRelic.addCustomParameter("ParallelBatch4", "yes");
            downstream.complete();
        }));
        promisesBatch.add(Promise.sync(() -> {
            NewRelic.addCustomParameter("ParallelBatch5", "yes");
            return "testing";
        }));

        AtomicInteger counter = new AtomicInteger(5);
        AtomicInteger actualInvocations = new AtomicInteger(0);

        ParallelBatch.of(promisesBatch).forEach((integer, s) -> {
            int count = counter.incrementAndGet();
            NewRelic.addCustomParameter("ParallelBatch" + count, "yes");

            if (actualInvocations.incrementAndGet() > promisesBatch.size()) {
                ctx.getResponse().status(500);
                System.out.println("Error: too many invocations");
            }
        }).then(() -> ctx.next());
    }

}
