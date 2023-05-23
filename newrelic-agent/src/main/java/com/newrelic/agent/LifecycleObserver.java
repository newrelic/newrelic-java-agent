package com.newrelic.agent;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.newrelic.agent.discovery.StatusMessageWriter;
import com.newrelic.agent.service.ServiceFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.newrelic.agent.config.IBMUtils;
import com.newrelic.agent.config.SystemPropertyFactory;
import com.newrelic.agent.discovery.AgentArguments;
import com.newrelic.agent.discovery.StatusClient;
import com.newrelic.agent.discovery.StatusMessage;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.bootstrap.BootstrapAgent;

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

    public boolean isAgentSafe() {
        return true;
    }

    public static LifecycleObserver createLifecycleObserver(String agentArgs) {
        if (agentArgs != null && !agentArgs.isEmpty()) {
            try {
                final AgentArguments args = AgentArguments.fromJsonObject(new JSONParser().parse(agentArgs));
                final Number port = args.getServerPort();
                StatusMessageWriter client = StatusClient.create(port.intValue());
                try {
                    client.write(StatusMessage.info(args.getId(), "Msg", "Initializing agent"));
                } catch (IOException io) {
                    // still create a lifecycle observer, even if we can't communicate our progress
                    client = message -> { };
                }
                return new AttachLifecycleObserver(client, args);
            } catch (ParseException | IOException e) {
                // ignore
            }
        }
        return new LifecycleObserver();
    }

    private static class AttachLifecycleObserver extends LifecycleObserver {

        private final StatusMessageWriter client;
        private final AtomicReference<ServiceManager> serviceManager = new AtomicReference<>();
        private final String id;

        public AttachLifecycleObserver(StatusMessageWriter client, AgentArguments args) {
            this.client = client;
            this.id = args.getId();
        }

        public boolean isAgentSafe() {
            if (IBMUtils.isIbmJVM() && 
                    !Boolean.parseBoolean(SystemPropertyFactory.getSystemPropertyProvider()
                            .getSystemProperty(BootstrapAgent.TRY_IBM_ATTACH_SYSTEM_PROPERTY))) {
                writeMessage(StatusMessage.error(id, "Error",
                        "The agent attach feature is not supported for IBM JVMs"));
                return false;
            }
            return true;
        }

        /**
         * Busy waits until the agent establishes a connection with New Relic.
         *
         * Under normal circumstances this can take several minutes. With {@code sync_startup: true} it should be nearly instantaneous.
         */
        @Override
        void agentStarted() {
            // retransform classes that use the custom Trace annotation
            new Thread(() -> ServiceFactory.getClassTransformerService().retransformForAttach()).start();

            writeMessage(StatusMessage.warn(id, "Msg",
                    "The agent has started and is connecting to New Relic. This may take a few minutes."));
            while (!writeConnectMessage()) {
                try {
                    TimeUnit.SECONDS.sleep(30);
                    writeMessage(StatusMessage.warn(id, "Msg", "Establishing a connection with New Relic..."));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Writes a message to an output stream containing the application URL when the agent has successfully connected to New Relic.
         *
         * @return true if the agent has successfully connected to New Relic, otherwise false
         */
        private boolean writeConnectMessage() {
            final ServiceManager serviceManager = this.serviceManager.get();
            if (serviceManager != null) {
                IRPMService rpmService = serviceManager.getRPMServiceManager().getRPMService();
                if (rpmService.isStoppedOrStopping()) {
                    writeMessage(StatusMessage.error(id, "Error", "The agent has shutdown. Make sure that the license key matches the region."));
                    return true;
                }
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
