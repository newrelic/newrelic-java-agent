import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.MetricsHelper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"org.apache.kafka.streams"})
public class KafkaStreams21SpansTest {
    @Rule
    public KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    private final String TOPIC = "life-universe-everything";
    private final String OUTPUT_TOPIC = "vogon-poetry";

    @Before
    public void before() {
        kafkaContainer.start();
    }

    @After
    public void after() {
        System.out.println(kafkaContainer.getLogs());
        kafkaContainer.stop();
    }

    @Test
    public void testStreams() throws ExecutionException, InterruptedException {
        sendMessages();
        runStreams();
        assertMetrics();

    }

    private void sendMessages() throws ExecutionException, InterruptedException {
        try (KafkaProducer<String, String> producer = KafkaStreamsHelper.newProducer(kafkaContainer)) {
            List<Future<RecordMetadata>> futures = Arrays.asList(
                    producer.send(new ProducerRecord<>(TOPIC, "Life, don't talk to me about life.")),
                    producer.send(new ProducerRecord<>(TOPIC, "Don't Panic")),
                    producer.send(new ProducerRecord<>(OUTPUT_TOPIC, "Oh freddled gruntbuggly"))
            );
            for (Future<RecordMetadata> future : futures) {
                future.get();
            }
        }
    }

    private void runStreams() throws InterruptedException {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> stream = builder.stream(TOPIC, Consumed.with(Serdes.String(), Serdes.String()));
        stream.to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));
        KafkaStreams kafkaStreams = KafkaStreamsHelper.newKafkaStreams(builder.build(), kafkaContainer);
        try {
            kafkaStreams.start();
            Thread.sleep(20000);
        } finally {
            kafkaStreams.close();
        }
    }

    private void assertMetrics() {
        String txnName = KafkaStreamsHelper.getTransactionName();
        assertUnscopedMetricExists(txnName);
        assertScopedMetricExists(txnName, "MessageBroker/Kafka/Streams/Task/AddRecords/ByPartition/Topic/Named/" + TOPIC);
    }

    private void assertScopedMetricExists(String txnName, String ...metricNames) {
        for (String metricName: metricNames) {
            int metricCount = MetricsHelper.getScopedMetricCount(txnName, metricName);
            assertTrue("metric not found: " + metricName, metricCount >= 1);
        }
    }

    private void assertUnscopedMetricExists(String ... metricNames) {
        Set<String> existingMetrics= InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().keySet();
        for (String metricName : metricNames) {
            Assert.assertTrue("metric not found: " + metricName, existingMetrics.contains(metricName));
        }
    }
}
