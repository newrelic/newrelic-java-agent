package org.apache.kafka.streams;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.streams.ClientIdToAppIdWithSuffixMap;
import com.nr.instrumentation.kafka.streams.StreamsSpansUtil;
import org.apache.kafka.streams.errors.StreamsException;

@Weave(originalName = "org.apache.kafka.streams.KafkaStreams")
public class KafkaStreams_Instrumentation {
    private final String clientId = Weaver.callOriginal();
    private final StreamsConfig applicationConfigs = Weaver.callOriginal();

    public synchronized void start() throws IllegalStateException, StreamsException {
        ClientIdToAppIdWithSuffixMap.put(clientId, StreamsSpansUtil.getAppIdWithClientIdSuffix(applicationConfigs));
        Weaver.callOriginal();
    }

    public void close() {
        String nrClientId = this.clientId;
        Weaver.callOriginal();
        ClientIdToAppIdWithSuffixMap.remove(nrClientId);
    }

}
