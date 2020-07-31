/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metrics;

import com.newrelic.agent.Transaction;

import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.Callable;

public class AsyncComponent implements Callable<Transaction> {

    private Queue<JsonTracer> tracers;
    private String myName;
    private final long startTime;
    private Stack<JsonTracer> myTracers;
    private boolean firstTracer = true;

    public AsyncComponent(String name, Queue<JsonTracer> pQueue, long initTime) {
        tracers = pQueue;
        myName = name;
        myTracers = new Stack<>();
        startTime = initTime;
    }

    @Override
    public Transaction call() throws Exception {
        Transaction.getTransaction();

        while (!tracers.isEmpty()) {
            JsonTracer current = tracers.peek();
            // names are unique
            if (current != null && myName.equals(current.getAsyncUnit())) {

                while (!myTracers.empty() && isDone(myTracers.peek(), current)) {
                    JsonTracer last = myTracers.pop();
                    last.finishTracer(startTime);
                }

                if (!myTracers.empty() && shouldEndTxEarly(myTracers.peek(), current)) {
                    myTracers.peek().endTx(startTime);

                }
                current.generateTracer(startTime, firstTracer);
                firstTracer = false;
                myTracers.push(current);
                tracers.poll();
            } else {
                Thread.sleep(1);
            }
        }

        while (!myTracers.empty()) {
            JsonTracer previous = myTracers.pop();
            previous.finishTracer(startTime);
        }

        return Transaction.getTransaction();
    }

    private boolean shouldEndTxEarly(JsonTracer onStack, JsonTracer current) {
        if (onStack.getEndTime() != null) {
            return current.getStartTime() > onStack.getEndTime();
        }
        return false;
    }

    private boolean isDone(JsonTracer onStack, JsonTracer current) {
        long endTime = onStack.getStartTime() + onStack.getDuration();
        return current.getStartTime() > endTime;
    }

}
