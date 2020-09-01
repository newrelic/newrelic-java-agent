package com.newrelic.agent.discovery;

import java.util.logging.Level;

public enum TerminalColor {
    Red("\u001B[31m"),
    Green("\u001B[32m"),
    Yellow("\u001B[33m"),
    Cyan("\u001B[36m");

    private static final String RESET = "\u001B[0m";

    private final String color;
    private TerminalColor(String color) {
        this.color = color;
    }

    public static TerminalColor fromLevel(Level level) {
        if (Level.SEVERE.equals(level)) {
            return Red;
        } else if (Level.WARNING.equals(level)) {
            return Yellow;
        }
        return Cyan;
    }

    public String colorText(String text) {
        return this.color + text + RESET;
    }

    public String formatMessage(String label, String message) {
        return colorText(label) + "\t| " + message;
    }
}
