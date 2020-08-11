package com.newrelic.agent.discovery;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Format the details about attach operations as json
 */
class JsonAttachOutput implements AttachOutput {
    private AttachInfo currentAttach;
    private List<AttachInfo> attaches = new ArrayList<>();
    private final PrintStream out;
    private final JsonSerializer serializer;

    public JsonAttachOutput(PrintStream out, JsonSerializer serializer) {
        this.out = out;
        this.serializer = serializer;
    }

    @Override
    public void attachStarted(String pid, String command, String agentArgs) {
        currentAttach = new AttachInfo(pid, command, agentArgs);
    }

    @Override
    public void write(StatusMessage message) {
        if (currentAttach != null) {
            currentAttach.addMessage(message);
        } else {
            System.err.println(message);
        }
    }

    @Override
    public void finished() {
        out.println(serializer.serialize(toArray(), false));
    }

    private Object toArray() {
        List<Object> arr = new ArrayList<>();
        for (AttachInfo info : attaches) {
            arr.add(info.toMap());
        }
        return arr;
    }

    @Override
    public void attachFinished() {
        attaches.add(currentAttach);
        currentAttach = null;
    }

    @Override
    public void error(Exception e) {
        // FIXME
        //e.printStackTrace();
    }

    @Override
    public void warn(String message) {
    }

    private static List<Map<String, String>> toPOJO(List<StatusMessage> messages) {
        List<Map<String, String>> list = new ArrayList<>();
        for (StatusMessage message : messages) {
            Map<String, String> map = new HashMap<>();
            map.put("level", message.getLevel().getName());
            map.put("label", message.getLabel());
            map.put("messsage", message.getMessage());
            list.add(map);
        }
        return list;
    }

    private static class AttachInfo {
        final String pid;
        final String command;
        final List<StatusMessage> messages = new ArrayList<>();
        final String agentArgs;
        boolean success = false;

        public AttachInfo(String pid, String command, String agentArgs) {
            this.pid = pid;
            this.command = command;
            this.agentArgs = agentArgs;
        }

        public void addMessage(StatusMessage message) {
            messages.add(message);
            success = success | message.isSuccess();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("pid", Integer.parseInt(pid));
            map.put("command", command);
            map.put("messages", toPOJO(messages));
            map.put("agentArgs", agentArgs);
            map.put("success", success);
            return map;
        }
    }
}
