package io.opentelemetry.sdk.resources;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.ExactClass)
public abstract class ResourceBuilder {
    public Resource build() {
        final String entityGuid = AgentBridge.getAgent().getEntityGuid(true);
        if (entityGuid != null) {
            put("entity.guid", entityGuid);
        }
        return Weaver.callOriginal();
    }

    public abstract ResourceBuilder put(String key, String value);
}
