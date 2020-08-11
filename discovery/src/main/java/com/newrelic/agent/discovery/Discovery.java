package com.newrelic.agent.discovery;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.VmIdentifier;

public class Discovery {
    static final String NEW_RELIC_LICENSE_KEY_ENV_NAME = "NEW_RELIC_LICENSE_KEY";
    private static final Set<String> PROCESS_SKIP_TERMS =
            Collections.unmodifiableSet(new HashSet<>(
                    Arrays.asList(
                            "com.ibm.ws.logging.hpel.viewer.LogViewer",
                            "sun.tools.jconsole.JConsole",
                            "gradle",
                            "newrelic")));

    static void discover(AttachOptions options) {
        final AttachOutput output = options.isJsonFormat() ?
            new JsonAttachOutput(System.out, options.getSerializer()) :
                new PlainAttachOutput();
        discover(options, output);
    }

    static void discover(AttachOptions options, final AttachOutput output) {
        StatusServer server = null;
        try {
            if (options.getPid() != null) {
                server = StatusServer.createAndStart(output);
                try {
                    attach(output, options.getSerializer(), getAgentJarPath(),
                            AgentArguments.getAgentArguments(options), server,
                            options.getPid(), options.getAppName(), null);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else {
                final VMConsumer vmConsumer;
                if (options.isList()) {
                    output.listHeader();
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
                            output.list(vmd.id(), vmd.displayName(), vmVersion, isAttachable);
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
        final AgentArguments agentArgs = AgentArguments.getAgentArguments(attachOptions);
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
            AgentArguments agentArgs, StatusServer server, String pid,
            String appName, String commandLine) {
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            try {
                agentArgs.update(appName, commandLine, server.getPort(), pid);
                final String args = serializer.serialize(agentArgs, true);
                attachOutput.attachStarted(pid, commandLine, args);
                vm.loadAgent(agentJarPath, args);
            } finally {
                vm.detach();
            }
        } catch (IOException e) {
            if (e.getMessage().contains("Non-numeric value found")) {
                attachOutput.write(StatusMessage.warn(pid, "Warning", "Unable to parse the attach response.  This may be a JVM bug."));
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
        // REVIEW not sure if we can skip all of these
        return displayName.isEmpty();
    }

    private interface VMConsumer {
        void consume(VirtualMachineDescriptor vmd);
    }
}
