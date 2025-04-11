package org.springframework.kafka.listener;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.spring.kafka.SpringKafkaUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.support.Acknowledgment;

@Weave(originalName = "org.springframework.kafka.listener.GenericMessageListener", type= MatchType.Interface)
public class GenericMessageListener_Instrumentation<T> {

    @Trace(dispatcher = true)
    public void onMessage(T data) {
        SpringKafkaUtil.processMessageListener(data);
        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public void onMessage(T data, Acknowledgment acknowledgment) {
        SpringKafkaUtil.processMessageListener(data);
        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public void onMessage(T data, Consumer<?, ?> consumer) {
        SpringKafkaUtil.processMessageListener(data);
        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public void onMessage(T data, Acknowledgment acknowledgment, Consumer<?, ?> consumer) {
        SpringKafkaUtil.processMessageListener(data);
        Weaver.callOriginal();
    }
}
