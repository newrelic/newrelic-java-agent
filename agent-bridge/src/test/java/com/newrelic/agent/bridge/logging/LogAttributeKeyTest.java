package com.newrelic.agent.bridge.logging;

import org.junit.Assert;
import org.junit.Test;

public class LogAttributeKeyTest {
    @Test
    public void getKey_returnsKey() {
        LogAttributeKey logAttributeKey = new LogAttributeKey("key", LogAttributeType.AGENT);
        Assert.assertEquals("key", logAttributeKey.getKey());
    }

    @Test
    public void getPrefixedKey_withAgentLogAttributeType_addsEmptyPrefix() {
        LogAttributeKey logAttributeKey = new LogAttributeKey("key", LogAttributeType.AGENT);
        Assert.assertEquals("key", logAttributeKey.getPrefixedKey());
    }

    @Test
    public void getPrefixedKey_withContextLogAttributeType_addsContextPrefix() {
        LogAttributeKey logAttributeKey = new LogAttributeKey("key", LogAttributeType.CONTEXT);
        Assert.assertEquals("context.key", logAttributeKey.getPrefixedKey());
    }

    @Test
    public void getType_returnsType() {
        LogAttributeKey logAttributeKey = new LogAttributeKey("key", LogAttributeType.AGENT);
        Assert.assertEquals(LogAttributeType.AGENT, logAttributeKey.getType());
    }

    @Test
    public void equals_properlyChecksEquality() {
        LogAttributeKey logAttributeKey1 = new LogAttributeKey("key", LogAttributeType.AGENT);
        LogAttributeKey logAttributeKey2 = new LogAttributeKey("key", LogAttributeType.AGENT);

        Assert.assertEquals(logAttributeKey1, logAttributeKey2);
    }
}
