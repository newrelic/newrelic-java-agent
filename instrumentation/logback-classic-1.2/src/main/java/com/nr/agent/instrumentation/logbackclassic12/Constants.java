package com.nr.agent.instrumentation.logbackclassic12;

public enum Constants {
    LOG_LINE("Logging/lines"),
    LOG_LINE_LEVEL("Logging/lines/%s");

    private String val;

    Constants(String val) {
        this.val = val;
    }

    public String getVal() {
        return val;
    }
}
