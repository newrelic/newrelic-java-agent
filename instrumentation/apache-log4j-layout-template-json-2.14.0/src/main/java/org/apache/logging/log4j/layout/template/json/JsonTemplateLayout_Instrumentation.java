package org.apache.logging.log4j.layout.template.json;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.logging.log4j.core.LogEvent;

import static com.nr.agent.instrumentation.log4j2.layout.template.json.AgentUtils.writeLinkingMetadata;

@Weave(originalName = "org.apache.logging.log4j.layout.template.json.JsonTemplateLayout", type = MatchType.ExactClass)
public class JsonTemplateLayout_Instrumentation {

    public String toSerializable(final LogEvent event) {
        String jsonString =  Weaver.callOriginal();
        if (jsonString != null) {
            StringBuilder sb = new StringBuilder(jsonString);
            writeLinkingMetadata(event, sb);
            jsonString = sb.toString();;
        }
        return jsonString;
    }

}
