package notjakarta.servlet.http;


import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import jakarta.servlet.ServletException;

import com.newrelic.agent.introspec.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "jakarta.servlet", configName = "debug.yml")
public class HttpServletInstrumentationTest {

    @Test
    public void doGetShouldAddMetricsToTransaction() throws InterruptedException, ServletException, IOException {

        String currentMethod = "GET";
        String transactionName = "WebTransaction/Uri/Unknown";

        startTx(currentMethod);
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);

        assertEquals(1, traces.size());
        assertEquals(2, metricsForTransaction.size());
        assertTrue(metricsForTransaction.containsKey("Java/notjakarta.servlet.http.MyServlet/doGet"));
    }

    @Test
    public void doPutShouldAddMetricsToTransaction() throws InterruptedException, ServletException, IOException {

        String currentMethod = "PUT";
        String transactionName = "WebTransaction/Uri/Unknown";

        startTx(currentMethod);
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);

        assertEquals(1, traces.size());
        assertEquals(2, metricsForTransaction.size());
        assertTrue(metricsForTransaction.containsKey("Java/notjakarta.servlet.http.MyServlet/doPut"));
    }

    @Test
    public void doPostShouldAddMetricsToTransaction() throws InterruptedException, ServletException, IOException {

        String currentMethod = "POST";
        String transactionName = "WebTransaction/Uri/Unknown";

        startTx(currentMethod);
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);

        assertEquals(1, traces.size());
        assertEquals(2, metricsForTransaction.size());
        assertTrue(metricsForTransaction.containsKey("Java/notjakarta.servlet.http.MyServlet/doPost"));
    }

    @Test
    public void doDeleteShouldAddMetricsToTransaction() throws InterruptedException, ServletException, IOException {

        String currentMethod = "DELETE";
        String transactionName = "WebTransaction/Uri/Unknown";

        startTx(currentMethod);
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);

        assertEquals(1, traces.size());
        assertEquals(2, metricsForTransaction.size());
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
