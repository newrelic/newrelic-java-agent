package org.apache.kafka.clients.consumer.internals;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.NewRelicMetricsReporter;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.metrics.Metrics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Weave(originalName = "org.apache.kafka.clients.consumer.internals.LegacyKafkaConsumer")
public class LegacyKafkaConsumer_Instrumentation<K, V> {
    private final Metrics metrics = Weaver.callOriginal();

    private final ConsumerMetadata metadata = Weaver.callOriginal();

    @NewField
    private boolean initialized;

    @WeaveAllConstructors
    public LegacyKafkaConsumer_Instrumentation() {
        if (!initialized) {
            List<Node> nodes = metadata.fetch().nodes();
            Set<String> nodeNames = new HashSet<>(nodes.size());
            for (Node node : nodes) {
                nodeNames.add(node.host() + ":" + node.port());
            }
            metrics.addReporter(new NewRelicMetricsReporter(nodeNames, NewRelicMetricsReporter.Mode.CONSUMER));
            initialized = true;
        }
    }
}
