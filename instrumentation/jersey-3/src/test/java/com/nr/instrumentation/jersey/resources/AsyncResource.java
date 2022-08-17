/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jersey.resources;

import com.newrelic.api.agent.NewRelic;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.container.TimeoutHandler;
import java.util.concurrent.*;

@Path("/async")
public class AsyncResource {

    @GET
    @Path("/resume")
    public void resume(@DefaultValue("1") @QueryParam("sleep") final int sleepMillis,
                       @Suspended final AsyncResponse response) {
        
        NewRelic.setTransactionName("AsyncResource", "resume");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = veryExpensiveOperation(sleepMillis);
                response.resume(result);
            }

            private String veryExpensiveOperation(int sleepMillis) {
                doWork(sleepMillis);
                return "async resume at " + System.currentTimeMillis();
            }
        }).start();
    }

    @GET
    @Path("/resumeWithIOException")
    @SyntheticIOException
    public void resumeWithIOException(@DefaultValue("1") @QueryParam("sleep") final int sleepMillis,
                                      @Suspended final AsyncResponse response) {

        NewRelic.setTransactionName("AsyncResource", "resumeWithIOException");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = veryExpensiveOperation(sleepMillis);
                response.resume(result);
            }

            private String veryExpensiveOperation(int sleepMillis) {
                doWork(sleepMillis);
                return "async resume at " + System.currentTimeMillis();
            }
        }).start();
    }

    @GET
    @Path("/syncEndpoint")
    public String syncEndpoint(@DefaultValue("1") @QueryParam("sleep") final int sleepMillis) {
        NewRelic.setTransactionName("AsyncResource", "syncEndpoint");
        return "syncEndpoint";
    }

    @GET
    @Path("/syncEndpointWithIOException")
    @SyntheticIOException
    public String syncEndpointWithIOException(@DefaultValue("1") @QueryParam("sleep") final int sleepMillis) {
        NewRelic.setTransactionName("AsyncResource", "syncEndpointWithIOException");
        return "syncEndpointWithIOException";
    }

    @GET
    @Path("/multipleResume")
    public void multipleResume(@DefaultValue("1") @QueryParam("sleep") final int sleepMillis,
                               @Suspended final AsyncResponse response) throws Exception {

        NewRelic.setTransactionName("AsyncResource", "multipleResume");
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        final CountDownLatch latch = new CountDownLatch(1);
        Future<?> resume = executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = veryExpensiveOperation(sleepMillis);
                try {
                    latch.await();
                } catch (InterruptedException e) {
                }
                response.resume(result);
            }

            private String veryExpensiveOperation(int sleepMillis) {
                doWork(sleepMillis);
                return "async resume at " + System.currentTimeMillis();
            }
        });

        Future<?> cancel = executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                }
                response.resume("Hi");
            }
        });

        latch.countDown();
        resume.get();
        cancel.get();
    }

    @GET
    @Path("/resumeThrowable")
    public void resumeThrowable(@DefaultValue("1") @QueryParam("sleep") final int sleepMillis,
                       @Suspended final AsyncResponse response) {

        NewRelic.setTransactionName("AsyncResource", "resumeThrowable");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = veryExpensiveOperation(sleepMillis);
                response.resume(new IllegalStateException("ResumeThrowable"));
            }

            private String veryExpensiveOperation(int sleepMillis) {
                doWork(sleepMillis);
                return "async throwable resume at " + System.currentTimeMillis();
            }
        }).start();
    }

    @GET
    @Path("/cancel")
    public void cancel(@DefaultValue("1") @QueryParam("sleep") final int sleepMillis,
                       @Suspended final AsyncResponse response) {
        NewRelic.setTransactionName("AsyncResource", "cancel");

        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = veryExpensiveOperation(sleepMillis);
                response.cancel();
            }

            private String veryExpensiveOperation(int sleepMillis) {
                doWork(sleepMillis);
                return "async cancel at " + System.currentTimeMillis();
            }
        }).start();
    }

    @GET
    @Path("/timeout")
    public void timeout(@DefaultValue("1") @QueryParam("sleep") final int sleepMillis,
                       @Suspended final AsyncResponse response) {

        NewRelic.setTransactionName("AsyncResource", "timeout");
        System.out.println("Timeout set: " + response.setTimeout(1L, TimeUnit.SECONDS));
        response.setTimeoutHandler(new TimeoutHandler() {
            @Override
            public void handleTimeout(AsyncResponse response) {
                response.resume("Request Timed Out");
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = veryExpensiveOperation((int) TimeUnit.SECONDS.toMillis(10));
                response.resume(result);
            }

            private String veryExpensiveOperation(int sleepMillis) {
                doWork(sleepMillis);
                return "async timeout at " + System.currentTimeMillis();
            }
        }).start();
    }

    @GET
    @Path("/timeoutThrowable")
    public void timeoutThrowable(@DefaultValue("1") @QueryParam("sleep") final int sleepMillis,
                        @Suspended final AsyncResponse response) {

        NewRelic.setTransactionName("AsyncResource", "timeoutThrowable");
        System.out.println("Timeout set: " + response.setTimeout(1L, TimeUnit.SECONDS));
        response.setTimeoutHandler(new TimeoutHandler() {
            @Override
            public void handleTimeout(AsyncResponse response) {
                response.resume(new TimeoutException("Timeout occurred!"));
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = veryExpensiveOperation((int) TimeUnit.SECONDS.toMillis(10));
                response.resume(result);
            }

            private String veryExpensiveOperation(int sleepMillis) {
                doWork(sleepMillis);
                return "async timeout at " + System.currentTimeMillis();
            }
        }).start();
    }

    @GET
    @Path("/timeoutCancel")
    public void timeoutCancel(@DefaultValue("1") @QueryParam("sleep") final int sleepMillis,
                                 @Suspended final AsyncResponse response) {

        NewRelic.setTransactionName("AsyncResource", "timeoutCancel");
        System.out.println("Timeout set: " + response.setTimeout(1L, TimeUnit.SECONDS));
        response.setTimeoutHandler(new TimeoutHandler() {
            @Override
            public void handleTimeout(AsyncResponse response) {
                response.cancel();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = veryExpensiveOperation((int) TimeUnit.SECONDS.toMillis(10));
                response.resume(result);
            }

            private String veryExpensiveOperation(int sleepMillis) {
                doWork(sleepMillis);
                return "async timeout at " + System.currentTimeMillis();
            }
        }).start();
    }

    /**
     * Do uninterruptible work without parking or sleeping.
     *
     * @param workMillis the number of milliseconds of uninterruptible work. If time appears to move backwards, this
     *                   method returns immediately.
     */
    private static void doWork(int workMillis) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + workMillis;

        long currentTime = System.currentTimeMillis();
        while (currentTime >= startTime && currentTime < endTime) {
            currentTime = System.currentTimeMillis();
        }
    }
}

