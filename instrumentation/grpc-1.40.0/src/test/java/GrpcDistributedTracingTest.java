
import app.TestClient;
import app.TestServer;
import com.newrelic.agent.introspec.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.grpc", "com.nr.agent.instrumentation.grpc" }, configName="enable_dist_tracing.yml")
public class GrpcDistributedTracingTest {

    private static TestServer server;
    private static TestClient client;

    @BeforeClass
    public static void before() throws Exception {
        server = new TestServer();
        server.start();
        client = new TestClient("localhost", server.getPort());
    }

    @AfterClass
    public static void after() throws InterruptedException {
        if (client != null) {
            client.shutdown();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testDTAsyncRequest() {
        client.helloAsync("Async");

        String clientTxName = "OtherTransaction/Custom/app.TestClient/helloAsync";
        String serverTxName = "WebTransaction/gRPC/helloworld.Greeter/SayHello";

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        assertEquals(2, introspector.getFinishedTransactionCount(30000));

        Collection<TransactionEvent> clientTxns = introspector.getTransactionEvents(clientTxName);
        assertEquals(1, clientTxns.size());
        String clientGuid = "";
        String clientTripId = "";
        for (TransactionEvent txn: clientTxns) {
            //make other assertions?
            clientGuid = txn.getMyGuid();
            clientTripId = txn.getTripId();
        }

        Collection<TransactionEvent> serverTxns = introspector.getTransactionEvents(serverTxName);
        assertEquals(1, serverTxns.size());
        for (TransactionEvent txn: serverTxns) {
            assertEquals(clientGuid, txn.getParentId());
            assertEquals(clientTripId, txn.getTripId());
        }
    }
}
