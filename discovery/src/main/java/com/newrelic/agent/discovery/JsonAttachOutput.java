package com.newrelic.agent.discovery;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Format the details about attach operations as json
 */
class JsonAttachOutput implements AttachOutput {
    private Map<String, AttachInfo> attaches = new ConcurrentHashMap<>();
    private List<ProcessInfo> processes = Collections.synchronizedList(new ArrayList<ProcessInfo>());
    private final PrintStream out;
    private final JsonSerializer serializer;

    public JsonAttachOutput(PrintStream out, JsonSerializer serializer) {
        this.out = out;
        this.serializer = serializer;
    }

    @Override
    public void attachStarted(String pid, String command, String agentArgs) {
        attaches.put(pid, new AttachInfo(pid, command, agentArgs));
    }

    @Override
    public void write(StatusMessage message) {
        AttachInfo attachInfo = attaches.get(message.getProcessId());
        if (attachInfo != null) {
            attachInfo.addMessage(message);
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
        for (AttachInfo info : attaches.values()) {
            arr.add(info.toMap());
        }
        for (ProcessInfo process : processes) {
            arr.add(process.toMap());
        }
        return arr;
    }

    @Override
    public void attachFinished() {
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
        final List<StatusMessage> messages = Collections.synchronizedList(new ArrayList<StatusMessage>());
        final String agentArgs;
        volatile boolean success = false;

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

    private static class ProcessInfo {
        final String id;
        final String displayName;
        final String vmVersion;
        final boolean isAttachable;

        public ProcessInfo(String id, String displayName, String vmVersion, boolean isAttachable) {
            this.id = id;
            this.displayName = displayName;
            this.vmVersion = vmVersion;
            this.isAttachable = isAttachable;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("pid", Integer.parseInt(id));
            map.put("displayName", displayName);
            map.put("vmVersion", vmVersion);
            map.put("attachable", isAttachable);
            return map;
        }
    }

    @Override
    public void listHeader() {
    }

    @Override
    public void list(String id, String displayName, String vmVersion, boolean isAttachable) {
        this.processes.add(new ProcessInfo(id, displayName, vmVersion, isAttachable));
    }
}
