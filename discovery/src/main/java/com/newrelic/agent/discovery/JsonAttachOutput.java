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
    private Map<String, ProcessInfo> processes = new ConcurrentHashMap<>();
    private final PrintStream out;
    private final JsonSerializer serializer;

    public JsonAttachOutput(PrintStream out, JsonSerializer serializer) {
        this.out = out;
        this.serializer = serializer;
    }

    @Override
    public void attachStarted(String pid, String command, String agentArgs) {
        processes.put(pid, new AttachInfo(pid, command, agentArgs));
    }

    @Override
    public void write(StatusMessage message) {
        ProcessInfo processInfo = processes.get(message.getProcessId());
        if (processInfo != null) {
            processInfo.addMessage(message);
        } else {
            System.err.println(message);
        }
    }

    @Override
    public void close() {
        out.println(serializer.serialize(toArray(), false));
    }

    @Override
    public void listHeader() {
    }

    @Override
    public void list(String id, String displayName, String vmVersion, boolean isAttachable) {
        this.processes.put(id, new DiscoveryInfo(id, displayName, vmVersion, isAttachable));
    }

    private Object toArray() {
        List<Object> arr = new ArrayList<>();
        for (ProcessInfo process : processes.values()) {
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

    private static class AttachInfo extends ProcessInfo {
        final String pid;
        final String command;
        final String agentArgs;

        public AttachInfo(String pid, String command, String agentArgs) {
            this.pid = pid;
            this.command = command;
            this.agentArgs = agentArgs;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = super.toMap();
            map.put("pid", Integer.parseInt(pid));
            map.put("command", command);
            map.put("agentArgs", agentArgs);
            map.put("success", success);
            return map;
        }
    }

    static class DiscoveryInfo extends ProcessInfo {
        final String id;
        final String displayName;
        final String vmVersion;
        final boolean isAttachable;
        volatile ApplicationContainerInfo applicationContainerInfo;

        public DiscoveryInfo(String id, String displayName, String vmVersion, boolean isAttachable) {
            this.id = id;
            this.displayName = displayName;
            this.vmVersion = vmVersion;
            this.isAttachable = isAttachable;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = super.toMap();
            map.put("pid", Integer.parseInt(id));
            map.put("displayName", displayName);
            map.put("vmVersion", vmVersion);
            map.put("attachable", isAttachable);
            if (applicationContainerInfo != null) {
                map.put("containerName", applicationContainerInfo.getContainerName());
                map.put("applicationNames", applicationContainerInfo.getApplicationNames());
            }
            return map;
        }
    }

    static abstract class ProcessInfo {
        final List<StatusMessage> messages = Collections.synchronizedList(new ArrayList<StatusMessage>());
        protected volatile boolean success;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            if (!messages.isEmpty()) {
                map.put("messages", toPOJO(messages));
            }
            return map;
        }

        public void addMessage(StatusMessage message) {
            messages.add(message);
            success = success | message.isSuccess();
        }
    }

    @Override
    public void applicationInfo(ApplicationContainerInfo applicationContainerInfo) {
        DiscoveryInfo discoveryInfo = (DiscoveryInfo) processes.get(applicationContainerInfo.getId());
        discoveryInfo.applicationContainerInfo = applicationContainerInfo;
    }
}
