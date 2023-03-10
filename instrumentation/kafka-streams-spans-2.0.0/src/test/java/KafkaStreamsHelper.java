import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.testcontainers.containers.KafkaContainer;

import java.util.Properties;

public class KafkaStreamsHelper {
    public static final String APPLICATION_ID = "test-streams-app";
    public static final String CLIENT_ID = "test-client-id";
    public static KafkaProducer<String, String> newProducer(KafkaContainer kafkaContainer) {
        Properties props = getProps(kafkaContainer.getBootstrapServers(), true);
        return new KafkaProducer<>(props);
    }

    public static KafkaStreams newKafkaStreams(Topology topology, KafkaContainer kafkaContainer) {
        Properties props = getProps(kafkaContainer.getBootstrapServers(), false);
        return new KafkaStreams(topology, props);
    }

    public static String getTransactionName() {
        return String.format("OtherTransaction/Message/Kafka/Streams/%s/%s", APPLICATION_ID, CLIENT_ID);
    }

    public static Properties getProps(String bootstrapServers, boolean isClientProps) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        if (isClientProps) {
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("group.id", "test-consumer-group");
        } else {
            props.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
            props.put(StreamsConfig.CLIENT_ID_CONFIG, CLIENT_ID);
        }
        return props;
    }

}
