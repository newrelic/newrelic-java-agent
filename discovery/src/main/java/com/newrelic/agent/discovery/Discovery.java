package com.newrelic.agent.discovery;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.VmIdentifier;

public class Discovery {
    public static final String NEW_RELIC_APP_NAME_ENV_VARIABLE = "NEW_RELIC_APP_NAME";
    public static final String NEW_RELIC_COMMAND_LINE_ENV_VARIABLE = "NEW_RELIC_COMMAND_LINE";
    public static final String SYSTEM_PROPERTIES_AGENT_ARGS_KEY = "properties";
    public static final String ENVIRONMENT_AGENT_ARGS_KEY = "environment";
    public static final String SERVER_PORT_AGENT_ARGS_KEY = "serverPort";
    private static final String NEW_RELIC_LICENSE_KEY_ENV_NAME = "NEW_RELIC_LICENSE_KEY";
    private static final Set<String> PROCESS_SKIP_TERMS =
            Collections.unmodifiableSet(new HashSet<>(
                    Arrays.asList(
                            "com.ibm.ws.logging.hpel.viewer.LogViewer",
                            "sun.tools.jconsole.JConsole",
                            "gradle",
                            "newrelic")));

    static void discover(AttachOptions options) {
        AttachOutput output = options.isJsonFormat() ?
                new JsonAttachOutput(System.out, options.getSerializer()) :
                    new PlainAttachOutput();
        StatusServer server = null;
        try {
            if (options.getPid() != null) {
                server = StatusServer.createAndStart(output);
                try {
                    attach(output, options.getSerializer(), getAgentJarPath(),
                            getAgentArgs(options), server,
                            options.getPid(), options.getAppName(), null);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else {
                final VMConsumer vmConsumer;
                if (options.isList()) {
                    System.out.println("Java processes:");
                    System.out.println("PID\tDisplay Name\tVM Version\tAttachable");
                    vmConsumer = new VMConsumer() {
        
                        @Override
                        public void consume(VirtualMachineDescriptor vmd) {
                            boolean isAttachable = false;
                            String vmVersion = "";
                            try {
                                VmIdentifier vmId = new VmIdentifier(vmd.id());
                                MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(vmId);
                                MonitoredVm monitoredVm = monitoredHost.getMonitoredVm(vmId, -1);
                                try {
                                    isAttachable = MonitoredVmUtil.isAttachable(monitoredVm);
                                    vmVersion = MonitoredVmUtil.vmVersion(monitoredVm);
                                } finally {
                                    monitoredHost.detach(monitoredVm);
                                }
                            } catch (URISyntaxException | MonitorException e) {
                                e.printStackTrace();
                            }
        
                            System.out.println(vmd.id() + '\t' + vmd.displayName() + '\t' +
                                    vmVersion + '\t' + isAttachable);
                        }
                    };
                } else {
                    server = StatusServer.createAndStart(output);
                    try {
                        vmConsumer = getAttachingVMConsumer(options, output,
                                options.getSerializer(), server);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
    
                final List<VirtualMachineDescriptor> vmds = VirtualMachine.list();
                for (VirtualMachineDescriptor vmd : vmds) {
                    if (!skip(vmd)) {
                        vmConsumer.consume(vmd);
                    }
                }
            }
        } finally {
            output.finished();
            if (server != null) {
                server.close();
            }
        }
    }

    private static VMConsumer getAttachingVMConsumer(
            final AttachOptions attachOptions,
            final AttachOutput attachOutput,
            final JsonSerializer serializer,
            final StatusServer server) throws URISyntaxException {
        final Map<String, Object> agentArgs = getAgentArgs(attachOptions);
        final String agentJarPath = getAgentJarPath();

        return new VMConsumer() {

            @Override
            public void consume(VirtualMachineDescriptor vmd) {
                attach(attachOutput, serializer, agentJarPath, agentArgs, server, vmd.id(),
                        null, getAppName(vmd.displayName()));
            }
        };
    }

    private static void attach(AttachOutput attachOutput, JsonSerializer serializer, String agentJarPath,
            Map<String, Object> agentArgs, StatusServer server, String pid,
            String appName, String commandLine) {
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            try {
                updateAgentArgs(agentArgs, appName, commandLine, server);
                final String args = serializer.serialize(agentArgs, true);
                attachOutput.attachStarted(pid, commandLine, args);
                vm.loadAgent(agentJarPath, args);
            } finally {
                vm.detach();
            }
        } catch (IOException e) {
            if (e.getMessage().contains("Non-numeric value found")) {
                attachOutput.write(StatusMessage.warn("Warning", "Unable to parse the attach response.  This may be a JVM bug."));
            } else {
                attachOutput.error(e);
            }
        } catch (Exception e) {
            attachOutput.error(e);
        } finally {
            if (server != null) {
                server.flush();
            }
            attachOutput.attachFinished();
        }
    }
    
    private static String getAppName(String displayName) {
        int index = displayName.lastIndexOf(File.separatorChar);
        return index > 0 ? displayName.substring(index + 1) : displayName;
    }

    private static void updateAgentArgs(Map<String, Object> args, String appName,
            String commandLine, StatusServer server) {
        @SuppressWarnings("unchecked")
        Map<String, String> env = (Map<String, String>) args.get(ENVIRONMENT_AGENT_ARGS_KEY);
        if (appName != null) {
            env.put(NEW_RELIC_APP_NAME_ENV_VARIABLE, appName);
        }
        if (commandLine != null) {
            env.put(NEW_RELIC_COMMAND_LINE_ENV_VARIABLE, commandLine);
        }
        if (server != null) {
            args.put(SERVER_PORT_AGENT_ARGS_KEY, server.getPort());
        }
    }

    private static Map<String, String> getEnvironment() {
        final Map<String, String> environment = new HashMap<>();
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().startsWith("NEW_RELIC_")) {
                String value = System.getenv(entry.getKey());
                if (value != null) {
                    environment.put(entry.getKey(), value);
                }
            }
        }
        return environment;
    }

    private static Map<String, String> getSystemProperties() {
        final Map<String, String> properties = new HashMap<>();
        for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
            if (entry.getKey().toString().startsWith("newrelic.")) {
                Object value = System.getProperties().get(entry.getKey());
                if (value != null) {
                    properties.put(entry.getKey().toString(), value.toString());
                }
            }
        }
        return properties;
    }

    private static Map<String, Object> getAgentArgs(AttachOptions attachOptions) {
        final Map<String, String> environment = getEnvironment();
        if (attachOptions.getLicenseKey() == null) {
            final String licenseKey = System.getenv(NEW_RELIC_LICENSE_KEY_ENV_NAME);
            if (licenseKey == null) {
                if (System.getProperty("newrelic.config.license_key") == null) {
                    throw new IllegalArgumentException("Please set the " + NEW_RELIC_LICENSE_KEY_ENV_NAME + " environment variable");
                }
            }
        } else {
            environment.put(NEW_RELIC_LICENSE_KEY_ENV_NAME, attachOptions.getLicenseKey());
        }
        Map<String, Object> args = new HashMap<>();
        args.put(ENVIRONMENT_AGENT_ARGS_KEY, environment);
        args.put(SYSTEM_PROPERTIES_AGENT_ARGS_KEY, getSystemProperties());
        return args;
    }

    static String getAgentJarPath() throws URISyntaxException {
        return new File(Discovery.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
    }

    private static boolean skip(VirtualMachineDescriptor vmd) {
        final String displayName = vmd.displayName();
        for (String term : PROCESS_SKIP_TERMS) {
            if (displayName.contains(term)) {
                return true;
            }
        }
        return // REVIEW not sure if we can skip all of these
                displayName.isEmpty();
    }

    private interface VMConsumer {
        void consume(VirtualMachineDescriptor vmd);
    }
}
