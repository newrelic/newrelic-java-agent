package com.newrelic.agent.discovery;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ApplicationContainerInfo implements Serializable, Externalizable {
    static final long serialVersionUID = 5800245423575764278L;
    private final String containerName;
    private final List<String> applicationNames;
    private final String id;

    public ApplicationContainerInfo(String id, String containerName, List<String> applicationNames) {
        this.id = id;
        this.containerName = containerName;
        this.applicationNames = applicationNames;
    }

    public String getId() {
        return id;
    }

    public String getContainerName() {
        return containerName;
    }

    public List<String> getApplicationNames() {
        return applicationNames;
    }

    @Override
    public String toString() {
        return "ApplicationContainerInfo [containerName=" + containerName + ", applicationNames=" + applicationNames
                + ", id=" + id + "]";
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(containerName);
        out.writeInt(applicationNames.size());
        for (String name : applicationNames) {
            out.writeUTF(name);
        }
    }

    public static ApplicationContainerInfo readExternal(ObjectInput in) throws IOException {
        String id = in.readUTF();
        String name = in.readUTF();
        int size = in.readInt();
        List<String> applicationNames = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            applicationNames.add(in.readUTF());
        }
        return new ApplicationContainerInfo(id, name, applicationNames);
    }
}
