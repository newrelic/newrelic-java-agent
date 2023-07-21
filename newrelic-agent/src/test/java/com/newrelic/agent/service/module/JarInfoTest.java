package com.newrelic.agent.service.module;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class JarInfoTest {

    @Test
    public void testStaticMissingJarInfo(){
        String expected = "JarInfo [version=" + JarCollectorServiceProcessor.UNKNOWN_VERSION + ", attributes={}]";
        assertEquals(expected, JarInfo.MISSING.toString());
    }

    @Test
    public void testToString(){
        Map<String, String> attributes = new HashMap<>();
        attributes.put("foo", "bar");
        attributes.put("baz", "lemon");
        JarInfo info = new JarInfo("9.0.0", attributes);

        String expected = "JarInfo [version=9.0.0, attributes={foo=bar, baz=lemon}]";
        assertEquals(expected, info.toString());

    }

    @Test
    public void testEqualsAndHashCode(){
        Map<String, String> attributes1 = new HashMap<>();
        attributes1.put("blep", "blop");
        attributes1.put("goob", "grog");

        Map<String, String> attributes2 = new HashMap<>();
        attributes2.put("blep", "blop");
        attributes2.put("goob", "grog");

        JarInfo jar1 = new JarInfo("1.0.1", attributes1);
        JarInfo jar2 = new JarInfo("1.0.1", attributes2);
        JarInfo jar3 = new JarInfo("1.0.1", null);
        JarInfo jar4 = new JarInfo("1.0.1", null);
        JarInfo jar5 = new JarInfo(null, null);
        JarInfo jar6 = new JarInfo(null, null);

        FakeJarInfo fakeJar = new FakeJarInfo("1.0.1", attributes1);

        assertEquals(jar1, jar1);

        assertEquals(jar1, jar2);
        assertEquals(jar2, jar1);
        assertEquals(jar1.hashCode(), jar2.hashCode());

        assertNotEquals(jar3, jar1);
        assertNotEquals(jar1, jar3);
        assertNotEquals(jar3.hashCode(), jar1.hashCode());

        assertEquals(jar3, jar4);
        assertEquals(jar4, jar3);
        assertEquals(jar3.hashCode(), jar4.hashCode());

        assertNotEquals(jar5, jar3);
        assertNotEquals(jar3, jar5);
        assertNotEquals(jar3.hashCode(), jar5.hashCode());

        assertEquals(jar5, jar6);
        assertEquals(jar6, jar5);
        assertEquals(jar5.hashCode(), jar6.hashCode());

        assertNotEquals(jar1, null);
        assertNotEquals(null, jar1);

        assertNotEquals(jar1, fakeJar);

    }

    private class FakeJarInfo {
        String version;
        Map<String, String> attributes;

        public FakeJarInfo(String fakeVersion, Map<String, String> fakeAttributes) {
            this.version = fakeVersion;
            this.attributes = fakeAttributes;
        }
    }

}