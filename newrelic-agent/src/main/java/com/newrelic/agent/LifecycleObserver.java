package com.newrelic.agent;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.newrelic.agent.autoname.ApplicationAutoName;
import com.newrelic.agent.discovery.AgentArguments;
import com.newrelic.agent.discovery.ApplicationContainerInfo;
import com.newrelic.agent.discovery.StatusClient;
import com.newrelic.agent.discovery.StatusMessage;
import com.newrelic.agent.service.ServiceManager;

/**
 * This class is used to communicate important startup information back to an attaching
 * process.
 */
public class LifecycleObserver {
    protected LifecycleObserver() {
    }

    void agentStarted() {
    }

    void serviceManagerStarted(ServiceManager serviceManager) {
    }

    void agentAlreadyRunning() {
    }

    boolean isDiscovery() {
        return false;
    }

    public static LifecycleObserver createLifecycleObserver(String agentArgs) {
        if (agentArgs != null && !agentArgs.isEmpty()) {
            try {
                final AgentArguments args = AgentArguments.fromJsonObject(new JSONParser().parse(agentArgs));
                final Number port = args.getServerPort();
                final StatusClient client = StatusClient.create(port.intValue());
                client.write(StatusMessage.info(args.getId(), "Msg",
                        args.isDiscover() ? "Discovering environment" : "Initializing agent"));
                return new AttachLifecycleObserver(client, args);
            } catch (ParseException | IOException e) {
                // ignore
            }
        }
        return new LifecycleObserver();
    }

    private static class AttachLifecycleObserver extends LifecycleObserver {

        private final StatusClient client;
        private final AtomicReference<ServiceManager> serviceManager = new AtomicReference<>();
        private final String id;
        private final boolean discovery;

        public AttachLifecycleObserver(StatusClient client, AgentArguments args) {
            this.client = client;
            this.id = args.getId();
            this.discovery = args.isDiscover();
            if (discovery) {
                ApplicationContainerInfo container = ApplicationAutoName.getApplicationContainerInfo(id);
                if (container != null) {
                    try {
                        client.write(container);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        boolean isDiscovery() {
            return discovery;
        }

        @Override
        void agentStarted() {
            if (!writeConnectMessage()) {
                writeMessage(StatusMessage.warn(id, "Msg",
                        "The agent started but was not able to connect to New Relic to send data"));
            }
        }

        private boolean writeConnectMessage() {
            final ServiceManager serviceManager = this.serviceManager.get();
            if (serviceManager != null) {
                IRPMService rpmService = serviceManager.getRPMServiceManager().getRPMService();
                if (rpmService.isConnected()) {
                    writeMessage(StatusMessage.success(id, rpmService.getApplicationLink()));
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
            writeMessage(StatusMessage.error(id, "Error", "The New Relic agent is already attached to this process"));
        }
    }
}
