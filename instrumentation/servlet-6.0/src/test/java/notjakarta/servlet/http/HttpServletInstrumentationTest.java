package notjakarta.servlet.http;


import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.test.marker.Java8IncompatibleTest;
import jakarta.servlet.ServletException;

import com.newrelic.agent.introspec.*;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "jakarta.servlet", configName = "debug.yml")
@Category({ Java8IncompatibleTest.class })
public class HttpServletInstrumentationTest {

    @Test
    public void doGetShouldAddMetricsForTransaction() throws InterruptedException, ServletException, IOException {

        String currentMethod = "GET";
        String transactionName = "WebTransaction/Uri/Unknown";

        startTx(currentMethod);
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        String methodInTrace = traces.iterator().next().getInitialTraceSegment().getChildren().get(0).getMethodName();

        assertEquals(1, traces.size());
        assertEquals(2, metricsForTransaction.size());
        assertEquals("doGet", methodInTrace);
        assertTrue(metricsForTransaction.containsKey("Java/notjakarta.servlet.http.MyServlet/doGet"));
    }

    @Test
    public void doPutShouldAddMetricsForTransaction() throws InterruptedException, ServletException, IOException {

        String currentMethod = "PUT";
        String transactionName = "WebTransaction/Uri/Unknown";

        startTx(currentMethod);
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        String methodInTrace = traces.iterator().next().getInitialTraceSegment().getChildren().get(0).getMethodName();

        assertEquals(1, traces.size());
        assertEquals(2, metricsForTransaction.size());
        assertEquals("doPut", methodInTrace);
        assertTrue(metricsForTransaction.containsKey("Java/notjakarta.servlet.http.MyServlet/doPut"));
    }

    @Test
    public void doPostShouldAddMetricsForTransaction() throws InterruptedException, ServletException, IOException {

        String currentMethod = "POST";
        String transactionName = "WebTransaction/Uri/Unknown";

        startTx(currentMethod);
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        String methodInTrace = traces.iterator().next().getInitialTraceSegment().getChildren().get(0).getMethodName();

        assertEquals(1, traces.size());
        assertEquals(2, metricsForTransaction.size());
        assertEquals("doPost", methodInTrace);
        assertTrue(metricsForTransaction.containsKey("Java/notjakarta.servlet.http.MyServlet/doPost"));
    }

    @Test
    public void doDeleteShouldAddMetricsForTransaction() throws InterruptedException, ServletException, IOException {

        String currentMethod = "DELETE";
        String transactionName = "WebTransaction/Uri/Unknown";

        startTx(currentMethod);
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        String methodInTrace = traces.iterator().next().getInitialTraceSegment().getChildren().get(0).getMethodName();

        assertEquals(1, traces.size());
        assertEquals(2, metricsForTransaction.size());
        assertEquals("doDelete", methodInTrace);
        assertTrue(metricsForTransaction.containsKey("Java/notjakarta.servlet.http.MyServlet/doDelete"));
    }

    private void startTx(String currentMethod) throws ServletException, IOException {

        MyServlet myServlet = new MyServlet();
        HttpServletTestRequest request = new HttpServletTestRequest();
        HttpServletTestResponse response = new HttpServletTestResponse();

        request.setCurrentMethod(currentMethod);
        myServlet.service(request, response);
    }

}
