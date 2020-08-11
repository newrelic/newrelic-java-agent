package com.newrelic.agent.discovery;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;

public class StatusMessage implements Serializable {
    // we use the RPM url as an indication of a successful attach operation
    private static final String URL_LABEL = "Url";
    static final long serialVersionUID = -1921492641455922593L;

    private final String id;
    private final Level level;
    private final String label;
    private final String message;

    public StatusMessage(String id, Level level, String label, String message) {
        this.id = id;
        this.level = level;
        this.label = label;
        this.message = message;
    }

    public static StatusMessage success(String id, String url) {
        return new StatusMessage(id, Level.INFO, URL_LABEL, url);
    }

    public static StatusMessage info(String id, String label, String message) {
        return new StatusMessage(id, Level.INFO, label, message);
    }

    public static StatusMessage error(String id, String label, String message) {
        return new StatusMessage(id, Level.SEVERE, label, message);
    }

    public static StatusMessage warn(String id, String label, String message) {
        return new StatusMessage(id, Level.WARNING, label, message);
    }

    public String getLabel() {
        return label;
    }

    public String getMessage() {
        return message;
    }

    public Level getLevel() {
        return level;
    }

    public String getProcessId() {
        return id;
    }

    @Override
    public String toString() {
        return TerminalColor.fromLevel(level).formatMessage(label, message);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(level.getName());
        out.writeUTF(label);
        out.writeUTF(message);
    }

    public static StatusMessage readExternal(ObjectInput in) throws IOException {
        return new StatusMessage(in.readUTF(), Level.parse(in.readUTF()), in.readUTF(), in.readUTF());
    }

    public boolean isSuccess() {
        return URL_LABEL.equals(label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, level, message);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StatusMessage other = (StatusMessage) obj;
        return Objects.equals(id, other.id) && Objects.equals(label, other.label) && Objects.equals(level, other.level)
                && Objects.equals(message, other.message);
    }
}
