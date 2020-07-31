/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.test.marker.Java7IncompatibleTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category({ Java7IncompatibleTest.class })
public class CompletableFutureTest {

    private static final String CATEGORY = "CompletableFuture";
    private static final String NAME = "future";
    private static final String TRANSACTION_NAME = String.format("OtherTransaction/%s/%s", CATEGORY, NAME);

    private void pause(long p) {
        synchronized (this) {
            try {
                wait(p);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void setTransactionName() {
        NewRelic.setTransactionName(CATEGORY, NAME);
    }

    @Test
    public void testAnyOf() throws Exception {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);

        CompletableFuture<?> future = doAnyOf();
        future.get();
        txs.waitFor(1, 5000); // Give the transaction time to finish

        assertEquals(1, txs.size());

        TransactionData txData = txs.get(0);
        Map<String, Object> userAttributes = txData.getUserAttributes();
        assertNotNull(userAttributes);
        assertEquals(3, userAttributes.size());
        assertEquals(1, userAttributes.get("req1"));
        assertEquals(2, userAttributes.get("req2"));
        assertEquals(3, userAttributes.get("req3"));
    }

    @Trace(dispatcher = true)
    public CompletableFuture<?> doAnyOf() throws Exception {
        CompletableFuture<String> req1 = supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                pause(5);
                NewRelic.addCustomParameter("req1", 1);
                return "1";
            }
        });

        CompletableFuture<String> req2 = supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                pause(15);
                NewRelic.addCustomParameter("req2", 2);
                return "2";
            }
        });

        CompletableFuture<String> req3 = supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                pause(10);
                NewRelic.addCustomParameter("req3", 3);
                return "3";
            }
        });

        CompletableFuture<Object> fastest = CompletableFuture.anyOf(req1, req2, req3);
        assertNotNull(fastest);
        return fastest;
    }

    @Test
    public void testApplyEither() throws Exception {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);

        CompletableFuture<Integer> future = doApplyEither();
        Integer result = future.get();
        assertNotNull(result);
        assertEquals(4, (int) result);
        txs.waitFor(1, 5000); // Give the transaction time to finish

        assertEquals(1, txs.size());

        TransactionData txData = txs.get(0);
        Map<String, Object> userAttributes = txData.getUserAttributes();
        assertNotNull(userAttributes);
        assertEquals(3, userAttributes.size());
        assertEquals(1, userAttributes.get("f1"));
        assertEquals(2, userAttributes.get("f2"));
        assertEquals(4, userAttributes.get("f3"));
    }

    @Trace(dispatcher = true)
    public CompletableFuture<Integer> doApplyEither() throws Exception {
        CompletableFuture<Integer> f1 = supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                pause(15);
                NewRelic.addCustomParameter("f1", 1);
                return 2;
            }
        });
        CompletableFuture<Integer> f2 = supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                pause(10);
                NewRelic.addCustomParameter("f2", 2);
                return 2;
            }
        });

        CompletableFuture<Integer> f3 = f1.applyToEither(f2, new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer r) {
                NewRelic.addCustomParameter("f3", r * r);
                return r * r; // This should always be 2 * 2
            }
        });

        return f3;
    }

    @Test
    public void testApplyAsync() throws Exception {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);

        CompletableFuture<Integer> future = doApplyAsync();
        Integer result = future.get();
        assertNotNull(result);
        assertEquals(2, (int) result);
        txs.waitFor(1, 5000); // Give the transaction time to finish

        assertEquals(1, txs.size());

        TransactionData txData = txs.get(0);
        Map<String, Object> userAttributes = txData.getUserAttributes();
        assertNotNull(userAttributes);
        assertEquals(2, userAttributes.size());
        assertEquals(2, userAttributes.get("b"));
        assertEquals(4, userAttributes.get("f"));
    }

    @Trace(dispatcher = true)
    public CompletableFuture<Integer> doApplyAsync() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        CompletableFuture<Integer> a = new CompletableFuture<>();
        final CompletableFuture<Integer> b = a.thenApplyAsync(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer r) {
                pause(10);
                NewRelic.addCustomParameter("b", r);
                latch.countDown();
                return r * r;
            }
        });

        CompletableFuture<Void> f = runAsync(new Runnable() {
            @Override
            public void run() {
                int x = b.join();
                NewRelic.addCustomParameter("f", x);
                latch.countDown();
            }
        });

        a.complete(2);
        latch.await();
        return a;
    }

    @Test
    public void testCompletableFutureError() throws Exception {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);

        CompletableFuture<Integer> future = doCompletableFutureError();
        Integer result = future.get();
        assertNotNull(result);
        assertEquals(3, (int) result);
        txs.waitFor(1, 5000); // Give the transaction time to finish

        assertEquals(1, txs.size());

        TransactionData txData = txs.get(0);
        Map<String, Object> userAttributes = txData.getUserAttributes();
        assertNotNull(userAttributes);
        assertEquals(2, userAttributes.size());
        assertEquals(1, userAttributes.get("f1"));
        // f2 is not here because the "exceptionally" function will get called instead
        assertEquals(3, userAttributes.get("e"));
    }

    @Trace(dispatcher = true)
    public CompletableFuture<Integer> doCompletableFutureError() throws Exception {
        CompletableFuture<Integer> f1 = supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                pause(10);
                NewRelic.addCustomParameter("f1", 1);
                if (true) {
                    throw new RuntimeException();
                }
                return 1;
            }
        });

        CompletableFuture<Integer> f2 = f1.thenApply(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer r) {
                NewRelic.addCustomParameter("f2", 2);
                return r * r;
            }
        }).exceptionally(new Function<Throwable, Integer>() {
            @Override
            public Integer apply(Throwable throwable) {
                NewRelic.addCustomParameter("e", 3);
                return 3;
            }
        });

        return f2;
    }

    @Test
    public void testCompletableFutureAccept() throws Exception {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);

        CompletableFuture<Integer> future = doCompletableFutureAccept();
        Integer result = future.join();
        assertNotNull(result);
        assertEquals(4, (int) result);
        txs.waitFor(1, 5000); // Give the transaction time to finish

        assertEquals(1, txs.size());

        TransactionData txData = txs.get(0);
        Map<String, Object> userAttributes = txData.getUserAttributes();
        assertNotNull(userAttributes);
        assertEquals(3, userAttributes.size());
        assertEquals(1, userAttributes.get("f1"));
        assertEquals(2, userAttributes.get("f2"));
        assertEquals(4, userAttributes.get("f3"));
    }

    @Trace(dispatcher = true)
    public CompletableFuture<Integer> doCompletableFutureAccept() throws Exception {
        CompletableFuture<Integer> f1 = supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                pause(10);
                NewRelic.addCustomParameter("f1", 1);
                if (false) {
                    throw new RuntimeException();
                }
                return 2;
            }
        });

        CompletableFuture<Integer> f2 = f1.thenApply(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer r) {
                NewRelic.addCustomParameter("f2", r);
                return r * r;
            }
        });

        CompletableFuture<Void> f3 = f2.thenAccept(new Consumer<Integer>() {
            @Override
            public void accept(Integer r) {
                NewRelic.addCustomParameter("f3", r);
            }
        });

        return f2;
    }

    @Test
    public void testLargeCompletableFuture() throws Exception {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);

        CompletableFuture<Integer> future = doLargeCompletableFuture();
        Integer result = future.get();
        assertNotNull(result);
        assertEquals(9, (int) result);
        txs.waitFor(1, 5000); // Give the transaction time to finish

        assertEquals(1, txs.size());

        TransactionData txData = txs.get(0);
        Map<String, Object> userAttributes = txData.getUserAttributes();
        assertNotNull(userAttributes);
        assertEquals(1, userAttributes.size());
        assertEquals(9, userAttributes.get("max"));
    }

    @Trace(dispatcher = true)
    public CompletableFuture<Integer> doLargeCompletableFuture() throws Exception {
        CompletableFuture<Integer> last = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return 0;
            }
        });

        for (int i = 1; i < 10; i++) {
            final int current = i;
            last = CompletableFuture.supplyAsync(new Supplier<Integer>() {
                @Override
                public Integer get() {
                    return current;
                }
            }).thenCombine(last, new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b) {
                    Integer result = Math.max(a, b);
                    NewRelic.addCustomParameter("max", result);
                    return result;
                }
            });
        }

        last.join();
        return last;
    }

    @Test
    public void testWhenComplete() throws Exception {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);

        CompletableFuture<Integer> future = doWhenComplete();
        Integer result = future.get();
        assertNotNull(result);
        assertEquals(4, (int) result);
        txs.waitFor(1, 1000); // Give the transaction time to finish

        assertEquals(1, txs.size());

        TransactionData txData = txs.get(0);
        Map<String, Object> userAttributes = txData.getUserAttributes();
        assertNotNull(userAttributes);
        assertEquals(3, userAttributes.size());
        assertEquals(1, userAttributes.get("f1"));
        assertEquals(4, userAttributes.get("f2"));
        assertEquals(2, userAttributes.get("f3"));
    }

    @Trace(dispatcher = true)
    public CompletableFuture<Integer> doWhenComplete() throws Exception {
        CompletableFuture<Integer> f1 = supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                pause(10);
                NewRelic.addCustomParameter("f1", 1);
                return 2;
            }
        });
        CompletableFuture<Integer> f2 = f1.handle(new BiFunction<Integer, Throwable, Integer>() {
            @Override
            public Integer apply(Integer res, Throwable throwable) {
                Integer result = throwable == null ? res * res : 2;
                NewRelic.addCustomParameter("f2", result);
                return result;
            }
        });
        CompletableFuture<Integer> f3 = f1.whenCompleteAsync(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer res, Throwable throwable) {
                NewRelic.addCustomParameter("f3", res);
            }
        });

        f2.get();
        f3.get();
        return f2;
    }

    @Test
    public void testThenCompose() throws Exception {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);

        CompletableFuture<Integer> future = doThenCompose();
        Integer result = future.get();
        assertNotNull(result);
        assertEquals(4, (int) result);
        txs.waitFor(1, 1000); // Give the transaction time to finish

        assertEquals(1, txs.size());

        TransactionData txData = txs.get(0);
        Map<String, Object> userAttributes = txData.getUserAttributes();
        assertNotNull(userAttributes);
        assertEquals(3, userAttributes.size());
        assertEquals(1, userAttributes.get("f1"));
        assertEquals(2, userAttributes.get("f2"));
        assertEquals(4, userAttributes.get("f3"));
    }

    @Trace(dispatcher = true)
    public CompletableFuture<Integer> doThenCompose() throws Exception {
        CompletableFuture<Integer> f1 = supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                pause(10);
                NewRelic.addCustomParameter("f1", 1);
                return 2;
            }
        });

        CompletableFuture<Integer> f2 = f1.thenComposeAsync(new Function<Integer, CompletionStage<Integer>>() {
            @Override
            public CompletionStage<Integer> apply(final Integer r) {
                NewRelic.addCustomParameter("f2", 2);
                return supplyAsync(new Supplier<Integer>() {
                    @Override
                    public Integer get() {
                        Integer result = r * r;
                        NewRelic.addCustomParameter("f3", result);
                        return result;
                    }
                });
            }
        });

        return f2;
    }

    @Test
    public void testAllOf() throws Exception {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);

        allOf();
        pause(500);
        AgentHelper.verifyMetrics(AgentHelper.getMetrics(), TRANSACTION_NAME);
        TransactionData transactionData = txs.get(0);
        Map<String, Object> userAttributes = transactionData.getUserAttributes();
        assertEquals(TRANSACTION_NAME, transactionData.getBlameMetricName());
        assertTrue(userAttributes.containsKey("one"));
        assertTrue(userAttributes.containsKey("two"));
        assertTrue(userAttributes.containsKey("three"));
    }

    @Trace(dispatcher = true)
    private void allOf() throws Exception {
        CompletableFuture<Void> futureOne = CompletableFuture.runAsync(
                new Runnable() {
                    @Override
                    public void run() {
                        pause(30);
                        NewRelic.addCustomParameter("one", "done");
                        setTransactionName();
                    }
                }
        );

        CompletableFuture<Void> futureTwo = CompletableFuture.runAsync(
                new Runnable() {
                    @Override
                    public void run() {
                        pause(20);
                        NewRelic.addCustomParameter("two", "done");
                    }
                }
        );

        CompletableFuture<Void> futureThree = CompletableFuture.runAsync(
                new Runnable() {
                    @Override
                    public void run() {
                        pause(10);
                        NewRelic.addCustomParameter("three", "done");
                    }
                }
        );

        CompletableFuture<Void> all = CompletableFuture.allOf(futureOne, futureTwo, futureThree);
        all.get();
    }

    @Test
    public void testAcceptEitherAsync() {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);

        acceptEitherAsync();
        pause(500);
        AgentHelper.verifyMetrics(AgentHelper.getMetrics(), TRANSACTION_NAME);

        TransactionData transactionData = txs.get(0);
        Map<String, Object> userAttributes = transactionData.getUserAttributes();
        assertTrue(userAttributes.containsKey("result"));
    }

    @Trace(dispatcher = true)
    public void acceptEitherAsync() {
        CompletableFuture<String> slowFuture = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                pause(100);
                return "Slow";
            }
        });

        CompletableFuture<String> fastFuture = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                return "Fast";
            }
        });

        slowFuture.acceptEitherAsync(fastFuture, new Consumer<String>() {
            @Override
            public void accept(String result) {
                setTransactionName();
                NewRelic.addCustomParameter("result", result);
            }
        });
    }

    @Test
    public void testAcceptBothAsync() {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);

        acceptBothAsync();
        pause(500);
        AgentHelper.verifyMetrics(AgentHelper.getMetrics(), TRANSACTION_NAME);

        TransactionData transactionData = txs.get(0);
        Map<String, Object> userAttributes = transactionData.getUserAttributes();
        assertTrue(userAttributes.containsKey("three"));
    }

    @Trace(dispatcher = true)
    private void acceptBothAsync() {
        CompletableFuture<Integer> one = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                setTransactionName();
                return 1;
            }
        });
        CompletableFuture<Integer> two = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return 2;
            }
        });

        one.thenAcceptBothAsync(two, new BiConsumer<Integer, Integer>() {
            @Override
            public void accept(Integer x, Integer y) {
                NewRelic.addCustomParameter("three", x + y);
            }
        });
    }

    @Test
    public void testCancelFuture() throws Exception {
        // Make sure a cancelled future doesn't hold up the transaction
        cancelFuture();
        pause(500);
        AgentHelper.verifyMetrics(AgentHelper.getMetrics(), TRANSACTION_NAME);
    }

    @Trace(dispatcher = true)
    private void cancelFuture() throws Exception {
        setTransactionName();

        final CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<Integer> one = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                try {
                    latch.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                }
                return 1;
            }
        });

        one.cancel(true); // boolean doesn't do anything
        latch.countDown();
        assertTrue(one.isCompletedExceptionally());
    }

    @Test
    public void testThenRunAsync() throws Exception {
        thenRunAsync();
        pause(500);
        AgentHelper.verifyMetrics(AgentHelper.getMetrics(), TRANSACTION_NAME);
    }

    @Trace(dispatcher = true)
    private void thenRunAsync() throws Exception {
        CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                pause(20);
                return 0;
            }
        }).thenRun(new Runnable() {
            @Override
            public void run() {
                pause(10);
                setTransactionName();
            }
        });
    }

    @Test
    public void testRunAfterEitherAsync() throws Exception {
        runAfterEitherAsync();
        pause(500);
        AgentHelper.verifyMetrics(AgentHelper.getMetrics(), TRANSACTION_NAME);
    }

    @Trace(dispatcher = true)
    private void runAfterEitherAsync() throws Exception {
        CompletableFuture<Integer> one = CompletableFuture.supplyAsync(
                new Supplier<Integer>() {
                    @Override
                    public Integer get() {
                        pause(20);
                        return 1;
                    }
                }

        );

        CompletableFuture<Integer> two = CompletableFuture.supplyAsync(
                new Supplier<Integer>() {
                    @Override
                    public Integer get() {
                        pause(30);
                        return 2;
                    }
                }

        );

        one.runAfterEitherAsync(two, new Runnable() {
                    @Override
                    public void run() {
                        setTransactionName();
                    }
                }
        );

    }

    @Test
    public void testHandleAsync() throws Exception {
        handleAsync();
        pause(500);
        AgentHelper.verifyMetrics(AgentHelper.getMetrics(), TRANSACTION_NAME);
    }

    @Trace(dispatcher = true)
    private void handleAsync() throws Exception {
        CompletableFuture<Integer> one = new CompletableFuture<>();
        one.handleAsync(new BiFunction<Integer, Throwable, Object>() {
            @Override
            public Integer apply(Integer integer, Throwable throwable) {
                setTransactionName();
                return 0;
            }
        });

        one.completeExceptionally(new RuntimeException());
    }
}
