package reactor.core.scheduler;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"reactor"})
public class CachedSchedulerTest {

    private Schedulers.CachedScheduler scheduler;

    private CountDownLatch latch;

    @Before
    public void setup() {
        Scheduler scheduler = Schedulers.fromExecutor(Executors.newSingleThreadExecutor());
        this.scheduler = new Schedulers.CachedScheduler("key", scheduler);
        latch = new CountDownLatch(1);
    }

    @After()
    public void tearDown() {
        this.scheduler.get().dispose();
        this.scheduler = null;
    }

    @Test
    public void testCachedScheduler() {
        runNow();
        try {
            boolean goodToGo = latch.await(1L, TimeUnit.SECONDS);
            assertTrue("Timed out waiting on latch", goodToGo);
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            fail("Thread was interrupted while running.");
        }
        verifyTraces("reactor.core.scheduler.CachedSchedulerTest/runNow");
    }

    @Trace(dispatcher = true)
    private void runNow() {
        scheduler.schedule(this::latchCountDown);
    }

    @Trace
    private void latchCountDown() {
        latch.countDown();
    }

    private void verifyTraces(String method) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String transactionName = "OtherTransaction/Custom/" + method;
        Map<String, TracedMetricData> scopedMetrics = introspector.getMetricsForTransaction(transactionName);
        assertEquals(2, scopedMetrics.size());
        assertTrue(scopedMetrics.containsKey("Java/" + method));
        assertTrue(scopedMetrics.containsKey("Custom/reactor.core.scheduler.CachedSchedulerTest/latchCountDown"));
    }

}
