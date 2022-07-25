package com.newrelic.agent.discovery;

import static org.junit.Assert.assertEquals;

import java.util.logging.Level;

import org.junit.Test;

public class TerminalColorTest {
    @Test
    public void fromLevelSevere() {
        assertEquals(TerminalColor.Red, TerminalColor.fromLevel(Level.SEVERE));
    }

    @Test
    public void fromLevelWarning() {
        assertEquals(TerminalColor.Yellow, TerminalColor.fromLevel(Level.WARNING));
    }

    @Test
    public void fromLevelOthers() {
        assertEquals(TerminalColor.Cyan, TerminalColor.fromLevel(Level.INFO));
        assertEquals(TerminalColor.Cyan, TerminalColor.fromLevel(Level.FINEST));
    }

    @Test
    public void colorTestRed() {
        assertEquals("[31mRed[0m", TerminalColor.Red.colorText("Red"));
    }

    @Test
    public void colorTestCyan() {
        assertEquals("[36mCyan[0m", TerminalColor.Cyan.colorText("Cyan"));
    }

    @Test
    public void colorTestYellow() {
        assertEquals("[33mYellow[0m", TerminalColor.Yellow.colorText("Yellow"));
    }
}
