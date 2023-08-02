package com.newrelic.agent.extension.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MethodMapperTest {

    MethodMapper mapper;

    @Before
    public void setup(){
        this.mapper = new MethodMapper();
    }

    @Test
    public void addIfNotPresent_newMethods_returnsTrue(){
        assertTrue(mapper.addIfNotPresent("foo", "this is the foo method"));
        assertTrue(mapper.addIfNotPresent("elephant", "this is a different method"));
    }

    @Test
    public void addIfNotPresent_newDescriptorsForSameMethod_returnsTrue(){
        assertTrue(mapper.addIfNotPresent("foo", "first description"));
        assertTrue(mapper.addIfNotPresent("foo", "second description"));
    }

    @Test
    public void addIfNotPresent_existingDescriptors_returnsFalse(){
        mapper.addMethod("foo", Arrays.asList("bar", "elephant", "kayak"));
        assertFalse(mapper.addIfNotPresent("foo", "elephant"));
        assertFalse(mapper.addIfNotPresent("foo", "kayak"));
        assertFalse(mapper.addIfNotPresent("foo", "bar"));
        assertTrue(mapper.addIfNotPresent("foo", "lemony"));
    }

    @Test
    public void testClear(){
        mapper.addMethod("foo", Arrays.asList("a", "b"));
        mapper.addMethod("buzz", Arrays.asList("x"));

        assertFalse(mapper.addIfNotPresent("foo", "a"));
        assertFalse(mapper.addIfNotPresent("foo", "b"));
        assertFalse(mapper.addIfNotPresent("buzz", "x"));

        mapper.clear();

        assertTrue(mapper.addIfNotPresent("foo", "a"));
        assertTrue(mapper.addIfNotPresent("foo", "b"));
        assertTrue(mapper.addIfNotPresent("buzz", "x"));
    }

    @Test
    public void addMethod_updates_existingDescriptors(){
        mapper.addMethod("foo", Arrays.asList("x"));
        mapper.addMethod("foo", Arrays.asList("y", "z"));

        assertFalse(mapper.addIfNotPresent("foo", "x"));
        assertFalse(mapper.addIfNotPresent("foo", "y"));
        assertFalse(mapper.addIfNotPresent("foo", "z"));
    }

}