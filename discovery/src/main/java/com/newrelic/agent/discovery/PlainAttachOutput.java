package com.newrelic.agent.discovery;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Joiner;
import com.newrelic.agent.discovery.JsonAttachOutput.DiscoveryInfo;

class PlainAttachOutput implements AttachOutput {
    private final PrintStream out;
    private final PrintStream err;
    private final Map<String, DiscoveryInfo> processes = new ConcurrentHashMap<>();

    public PlainAttachOutput() {
        this(System.out, System.err);
    }

    public PlainAttachOutput(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public void attachStarted(String pid, String command, String agentArgs) {
        out.println(TerminalColor.Cyan.colorText("\n--- Attaching ---"));
        out.println(TerminalColor.Cyan.formatMessage("\tPID", pid));
        out.println(TerminalColor.Cyan.formatMessage("\tCommand", command));
    }

    @Override
    public void write(StatusMessage message) {
        if (processes.isEmpty()) {
            out.println('\t' + message.toString());
        }
    }

    @Override
    public void close() {
        for (DiscoveryInfo discoveryInfo : processes.values()) {
            System.out.println(
                    Joiner.on('\t').join(discoveryInfo.id, discoveryInfo.vmVersion,
                            discoveryInfo.isAttachable, discoveryInfo.displayName));

        }

        out.flush();
        err.flush();
    }

    @Override
    public void attachFinished() {
    }

    @Override
    public void error(Exception e) {
        e.printStackTrace(err);
    }

    @Override
    public void warn(String message) {
    }

    @Override
    public void listHeader() {
        System.out.println("Java processes:");
        System.out.println("PID\tVM Version\tAttachable\tDisplay Name\tServer Info\tApplication Names");
    }

    @Override
    public void list(String id, String displayName, String vmVersion, boolean isAttachable) {
        if (!isAttachable) {
            System.out.println(
                    Joiner.on('\t').join(id, vmVersion, isAttachable, displayName));
        } else {
            processes.put(id, new DiscoveryInfo(id, displayName, vmVersion, isAttachable));
        }
    }

    @Override
    public void applicationInfo(ApplicationContainerInfo applicationContainerInfo) {
        DiscoveryInfo discoveryInfo = processes.remove(applicationContainerInfo.getId());
        if (discoveryInfo != null) {
            System.out.println(
                    Joiner.on('\t').join(discoveryInfo.id, discoveryInfo.vmVersion,
                            discoveryInfo.isAttachable, discoveryInfo.displayName,
                            applicationContainerInfo.getContainerName(), applicationContainerInfo.getApplicationNames()));
        }
    }
}
