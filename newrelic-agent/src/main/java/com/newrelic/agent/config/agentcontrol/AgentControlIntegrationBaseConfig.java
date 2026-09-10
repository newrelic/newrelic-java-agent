package com.newrelic.agent.config.agentcontrol;

import com.newrelic.agent.config.BaseConfig;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Map;

public abstract class AgentControlIntegrationBaseConfig extends BaseConfig {
    public AgentControlIntegrationBaseConfig(Map<String, Object> props, String systemPropertyPrefix) {
        super(props, systemPropertyPrefix);
    }

    protected DeliveryLocation validateAndAssignDeliveryLocation(String locationAsUri, URI defaultUri) {
        URI tmpUri;
        String tmpClientType;

        if (StringUtils.isNotEmpty(locationAsUri)) {
            try {
                tmpUri = new URI(locationAsUri);
                tmpClientType = tmpUri.getScheme();
            } catch (Exception e) {
                tmpUri = null;
                tmpClientType = null;
            }
        } else {
            tmpUri = defaultUri;
            tmpClientType = tmpUri.getScheme();
        }

        // Ensure the URI contains the scheme and path
        if (StringUtils.isAnyEmpty(tmpClientType, (tmpUri == null ? "":  tmpUri.getPath()))) {
            tmpClientType = null;
        }

        return new DeliveryLocation(tmpUri, tmpClientType);
    }

    protected static class DeliveryLocation {
        private final URI deliveryLocation;
        private final String clientType;

        public DeliveryLocation(URI deliveryLocation, String clientType) {
            this.deliveryLocation = deliveryLocation;
            this.clientType = clientType;
        }

        public URI getDeliveryLocation() {
            return deliveryLocation;
        }
        public String getClientType() {
            return clientType;
        }
    }
}
