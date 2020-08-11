package com.newrelic.agent.discovery;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.logging.Level;

public class StatusMessage implements Externalizable {
    // we use the RPM url as an indication of a successful attach operation
    private static final String URL_LABEL = "Url";
    private static final long serialVersionUID = -1921492641455922593L;

    public Level level;
    public String label;
    public String message;

    public StatusMessage(Level level, String label, String message) {
        this.level = level;
        this.label = label;
        this.message = message;
    }

    StatusMessage() {
    }

    public static StatusMessage success(String url) {
        return new StatusMessage(Level.INFO, URL_LABEL, url);
    }

    public static StatusMessage info(String label, String message) {
        return new StatusMessage(Level.INFO, label, message);
    }

    public static StatusMessage error(String label, String message) {
        return new StatusMessage(Level.SEVERE, label, message);
    }

    public static StatusMessage warn(String label, String message) {
        return new StatusMessage(Level.WARNING, label, message);
    }

    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return TerminalColor.fromLevel(level).formatMessage(label, message);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(label);
        out.writeUTF(message);
        out.writeUTF(level.getName());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        label = in.readUTF();
        message = in.readUTF();
        level = Level.parse(in.readUTF());
    }

    public boolean isSuccess() {
        return URL_LABEL.equals(label);
    }
}
