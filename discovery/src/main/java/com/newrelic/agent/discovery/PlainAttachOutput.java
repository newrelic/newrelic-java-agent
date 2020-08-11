package com.newrelic.agent.discovery;

import java.io.PrintStream;

class PlainAttachOutput implements AttachOutput {
    private final PrintStream out;
    private final PrintStream err;

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
        out.println('\t' + message.toString());
    }

    @Override
    public void finished() {
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
        System.out.println("PID\tDisplay Name\tVM Version\tAttachable");
    }

    @Override
    public void list(String id, String displayName, String vmVersion, boolean isAttachable) {
        System.out.println(id + '\t' + displayName + '\t' + vmVersion + '\t' + isAttachable);
    }
}
