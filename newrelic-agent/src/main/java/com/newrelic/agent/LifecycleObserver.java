package com.newrelic.agent;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.newrelic.agent.config.IBMUtils;
import com.newrelic.agent.config.SystemPropertyFactory;
import com.newrelic.agent.discovery.AgentArguments;
import com.newrelic.agent.discovery.StatusClient;
import com.newrelic.agent.discovery.StatusMessage;
import com.newrelic.bootstrap.BootstrapAgent;

/**
 * This class is used to communicate important startup information back to an attaching
 * process.
 */
public class LifecycleObserver extends AbstractService implements ConnectionListener {
    protected LifecycleObserver() {
        super(LifecycleObserver.class.getSimpleName());
    }

    public boolean isAgentSafe() {
        return true;
    }

    public static LifecycleObserver createLifecycleObserver(String agentArgs) {
        if (agentArgs != null && !agentArgs.isEmpty()) {
            try {
                final AgentArguments args = AgentArguments.fromJsonObject(new JSONParser().parse(agentArgs));
                final Number port = args.getServerPort();
                final StatusClient client = StatusClient.create(port.intValue());
                client.write(StatusMessage.info(args.getId(), "Msg", "Initializing agent"));
                return new AttachLifecycleObserver(client, args);

            } catch (ParseException | IOException e) {
                System.out.println("Unable to create lifecycle observer: " + e);
            }
        }
        return new LifecycleObserver();
    }

    @Override
    public void connected(IRPMService rpmService, AgentConfig agentConfig) {
    }

    @Override
    public void disconnected(IRPMService rpmService) {

    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private static class AttachLifecycleObserver extends LifecycleObserver {

        private final StatusClient client;
        private final String id;

        public AttachLifecycleObserver(StatusClient client, AgentArguments args) {
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

        public void doStart() {
            ServiceFactory.getRPMServiceManager().addConnectionListener(this);
        }

        /**
         * Busy waits until the agent establishes a connection with New Relic.
         *
         * Under normal circumstances this can take several minutes. With {@code sync_startup: true} it should be nearly instantaneous.
         */
        @Override
        public void connected(IRPMService rpmService, AgentConfig agentConfig) {
            writeMessage(StatusMessage.success(id, rpmService.getApplicationLink()));
        }

        private void writeMessage(StatusMessage message) {
            try {
                client.write(message);
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
