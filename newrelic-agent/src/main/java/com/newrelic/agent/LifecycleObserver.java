package com.newrelic.agent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.newrelic.agent.discovery.Discovery;
import com.newrelic.agent.discovery.StatusClient;
import com.newrelic.agent.discovery.StatusMessage;
import com.newrelic.agent.service.ServiceManager;

public class LifecycleObserver {
    protected LifecycleObserver() {
    }

    void agentStarted() {
    }

    void serviceManagerStarted(ServiceManager serviceManager) {
    }

    void agentAlreadyRunning() {
    }

    public static LifecycleObserver createLifecycleObserver(String agentArgs) {
        if (agentArgs != null && !agentArgs.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) new JSONParser().parse(agentArgs);
                final Number port = (Number) map.get(Discovery.SERVER_PORT_AGENT_ARGS_KEY);
                final StatusClient client = StatusClient.create(port.intValue());
                client.write(StatusMessage.info("Msg", "Initializing agent"));
                return new AttachLifecycleObserver(client);
            } catch (ParseException | IOException e) {
                // ignore
            }
        }
        return new LifecycleObserver();
    }

    private static class AttachLifecycleObserver extends LifecycleObserver {

        private final StatusClient client;
        private final AtomicReference<ServiceManager> serviceManager = new AtomicReference<>();

        public AttachLifecycleObserver(StatusClient client) {
            this.client = client;
        }

        @Override
        void agentStarted() {
            if (!writeConnectMessage()) {
                writeMessage(StatusMessage.info("Msg", "Agent started"));
            }
        }

        private boolean writeConnectMessage() {
            final ServiceManager serviceManager = this.serviceManager.get();
            if (serviceManager != null) {
                IRPMService rpmService = serviceManager.getRPMServiceManager().getRPMService();
                if (rpmService.isConnected()) {
                    writeMessage(StatusMessage.success(rpmService.getApplicationLink()));
                    return true;
                }
            }
            return false;
        }

        private void writeMessage(StatusMessage message) {
            try {
                System.out.println(message);
                client.write(message);
            } catch (IOException e) {
                // ignore
            }
        }

        @Override
        public void serviceManagerStarted(ServiceManager serviceManager) {
            this.serviceManager.set(serviceManager);
        }

        public void agentAlreadyRunning() {
            writeMessage(StatusMessage.error("Error", "The New Relic agent is already attached to this process"));
        }
    }
}
