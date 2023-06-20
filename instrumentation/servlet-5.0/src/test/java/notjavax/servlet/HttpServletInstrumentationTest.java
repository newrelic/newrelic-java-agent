package notjavax.servlet;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TransactionTrace;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "jakarta.servlet", configName = "debug.yml")
public class HttpServletInstrumentationTest {

    @Test
    public void doGetShouldAddToTraceCount() throws InterruptedException, ServletException, IOException {
        startTx();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        String transactionName = "WebTransaction/Uri/Unknown";
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
//        Thread.sleep(1000000000L);
        assertEquals(1, traces.size());
    }

    @Test
    public void doPostShouldAddToTraceCount() throws InterruptedException, ServletException, IOException {
        startTx();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        String transactionName = "WebTransaction/Uri/Unknown";
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
//        Thread.sleep(1000000000L);
        assertEquals(1, traces.size());
    }

    @Test
    public void doPutShouldAddToTraceCount() throws InterruptedException, ServletException, IOException {
        startTx();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        String transactionName = "WebTransaction/Uri/Unknown";
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
//        Thread.sleep(1000000000L);
        assertEquals(1, traces.size());
    }

    @Test
    public void doDeleteShouldAddToTraceCount() throws InterruptedException, ServletException, IOException {
        startTx();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        String transactionName = "WebTransaction/Uri/Unknown";
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
//        Thread.sleep(1000000000L);
        assertEquals(1, traces.size());
    }

    private void startTx() throws ServletException, IOException {
        MyServlet myServlet = new MyServlet();
        HttpServletTestRequest request = new HttpServletTestRequest();
        HttpServletResponse response = new HttpServletTestResponse();
        myServlet.service(request, response);
    }
}
