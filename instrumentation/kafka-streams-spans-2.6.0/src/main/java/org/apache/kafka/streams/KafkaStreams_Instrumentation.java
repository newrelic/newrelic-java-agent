package org.apache.kafka.streams;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.streams.ClientIdToAppIdMap;
import org.apache.kafka.streams.errors.StreamsException;

@Weave(originalName = "org.apache.kafka.streams.KafkaStreams")
public class KafkaStreams_Instrumentation {
    private final String clientId = Weaver.callOriginal();
    private final StreamsConfig config = Weaver.callOriginal();

    public synchronized void start() throws IllegalStateException, StreamsException {
        ClientIdToAppIdMap.put(clientId, config.getString(StreamsConfig.APPLICATION_ID_CONFIG));
        Weaver.callOriginal();
    }

    public void close() {
        String nrClientId = this.clientId;
        Weaver.callOriginal();
        ClientIdToAppIdMap.remove(nrClientId);
    }

}
