package com.newrelic.agent.config;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ParsedConfigValueTest {

    @Test
    public void stringConfigRetrievedWithoutDefault() {
        ParsedConfigValue configValue = new ParsedConfigValue("configured_value");
        assertEquals("configured_value", configValue.getParsedValue());
    }

    @Test
    public void stringConfigRetrievedAsString() {
        ParsedConfigValue configValue = new ParsedConfigValue("configured_value");
        String retrieved = configValue.getParsedValue("default_value");
        assertEquals("configured_value", retrieved);
    }

    @Test
    public void stringConfigRetrievedAsBoolean() {
        ParsedConfigValue configValue = new ParsedConfigValue("true");
        Boolean retrieved = configValue.getParsedValue(FALSE);
        assertEquals(TRUE, retrieved);
    }

    @Test
    public void stringConfigTreatedAsFalsyBoolean() {
        ParsedConfigValue configValue = new ParsedConfigValue("configured_value");
        Boolean retrieved = configValue.getParsedValue(TRUE);
        assertEquals(FALSE, retrieved);
    }

    @Test
    public void stringConfigRetrievedAsInteger() {
        ParsedConfigValue configValue = new ParsedConfigValue("123");
        Integer retrieved = configValue.getParsedValue(-1);
        assertEquals(new Integer(123), retrieved);
    }

    @Test
    public void stringConfigUnparseableAsInteger() {
        ParsedConfigValue configValue = new ParsedConfigValue("configured_value");
        Integer retrieved = configValue.getParsedValue(-1);
        assertEquals(new Integer(-1), retrieved);
    }

    @Test
    public void stringConfigRetrievedAsSingleItemList() {
        ParsedConfigValue configValue = new ParsedConfigValue("true");
        List<String> retrieved = configValue.getParsedValue(emptyList());
        assertEquals(singletonList("true"), retrieved);
    }

    @Test
    public void stringConfigRetrievedAsEmptyList() {
        ParsedConfigValue configValue = new ParsedConfigValue("    ");
        List<String> retrieved = configValue.getParsedValue(singletonList("default_value"));
        assertEquals(emptyList(), retrieved);
    }

    @Test
    public void stringConfigRetrievedAsMultipleItemList() {
        ParsedConfigValue configValue = new ParsedConfigValue("one, two, three");
        List<String> retrieved = configValue.getParsedValue(emptyList());
        assertEquals(asList("one", "two", "three"), retrieved);
    }

    @Test
    public void stringConfigRetrievedAsMap() {
        ParsedConfigValue configValue = new ParsedConfigValue("key1:value1; key 2: value 2");
        Map<String, String> retrieved = configValue.getParsedValue(emptyMap());
        Map<String, String> expected = new LinkedHashMap<>(2);
        expected.put("key1", "value1");
        expected.put("key 2", "value 2");
        assertEquals(expected, retrieved);
    }

    @Test
    public void unparseableStringConfigRetrievedAsMap() {
        ParsedConfigValue configValue = new ParsedConfigValue("key1");
        Map<String, String> defaultValue = singletonMap("key1", "default_value1");
        Map<String, String> retrieved = configValue.getParsedValue(defaultValue);
        assertEquals(defaultValue, retrieved);
    }

    @Test
    public void booleanConfigRetrievedWithoutDefault() {
        ParsedConfigValue configValue = new ParsedConfigValue(TRUE);
        assertEquals(TRUE, configValue.getParsedValue());
    }

    @Test
    public void booleanConfigRetrievedAsBoolean() {
        ParsedConfigValue configValue = new ParsedConfigValue(TRUE);
        assertEquals(TRUE, configValue.getParsedValue(FALSE));
    }

    @Test
    public void booleanConfigRetrievedAsString() {
        ParsedConfigValue configValue = new ParsedConfigValue(TRUE);
        String defaultValue = "OFF";
        String retrieved = configValue.getParsedValue(defaultValue);
        assertEquals(defaultValue, retrieved);
    }

    @Test
    public void booleanConfigRetrievedAsInteger() {
        ParsedConfigValue configValue = new ParsedConfigValue(TRUE);
        Integer defaultValue = 123;
        Integer retrieved = configValue.getParsedValue(defaultValue);
        assertEquals(defaultValue, retrieved);
    }

    @Test
    public void booleanConfigRetrievedAsList() {
        ParsedConfigValue configValue = new ParsedConfigValue(TRUE);
        List<String> defaultValue = singletonList("default_value");
        List<String> retrieved = configValue.getParsedValue(defaultValue);
        assertEquals(defaultValue, retrieved);
    }

    @Test
    public void booleanConfigRetrievedAsMap() {
        ParsedConfigValue configValue = new ParsedConfigValue(TRUE);
        Map<String, String> defaultValue = singletonMap("key1", "default value1");
        Map<String, String> retrieved = configValue.getParsedValue(defaultValue);
        assertEquals(defaultValue, retrieved);
    }

    @Test
    public void integerConfigRetrievedWithoutDefault() {
        ParsedConfigValue configValue = new ParsedConfigValue(123);
        assertEquals(new Integer(123), configValue.getParsedValue());
    }

    @Test
    public void integerConfigRetrievedAsInteger() {
        ParsedConfigValue configValue = new ParsedConfigValue(123);
        assertEquals(new Integer(123), configValue.getParsedValue(-1));
    }

    @Test
    public void integerConfigRetrievedAsBoolean() {
        ParsedConfigValue configValue = new ParsedConfigValue(123);
        Boolean defaultValue = TRUE;
        Boolean retrieved = configValue.getParsedValue(defaultValue);
        assertEquals(defaultValue, retrieved);
    }

    @Test
    public void integerConfigRetrievedAsString() {
        ParsedConfigValue configValue = new ParsedConfigValue(123);
        String defaultValue = "default_value";
        String retrieved = configValue.getParsedValue(defaultValue);
        assertEquals(defaultValue, retrieved);
    }

    @Test
    public void integerConfigRetrievedAsList() {
        ParsedConfigValue configValue = new ParsedConfigValue(123);
        List<String> defaultValue = singletonList("default_value");
        List<String> retrieved = configValue.getParsedValue(defaultValue);
        assertEquals(defaultValue, retrieved);
    }

    @Test
    public void integerConfigRetrievedAsMap() {
        ParsedConfigValue configValue = new ParsedConfigValue(123);
        Map<String, String> defaultValue = singletonMap("key1", "default value1");
        Map<String, String> retrieved = configValue.getParsedValue(defaultValue);
        assertEquals(defaultValue, retrieved);
    }

    @Test(expected = ClassCastException.class)
    public void invalidCastAttemptWithoutDefault() {
        ParsedConfigValue configValue = new ParsedConfigValue(123);
        String retrieved = configValue.getParsedValue();
        fail("Should cause ClassCastException");
    }

    @Test(expected = ClassCastException.class)
    public void invalidCastAttemptWithDefaultFailsOnAccessNotRetrieval() {
        ParsedConfigValue configValue = new ParsedConfigValue("configured_value");
        List<Integer> retrieved = configValue.getParsedValue(emptyList());
        assertEquals(singletonList("configured_value"), retrieved);
        Integer accessAttempt = retrieved.get(0);
        fail("Should cause ClassCastException");
    }

}
