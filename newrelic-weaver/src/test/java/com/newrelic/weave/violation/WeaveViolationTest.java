package com.newrelic.weave.violation;

import org.junit.Test;
import org.objectweb.asm.commons.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class WeaveViolationTest {
    @Test
    public void constructor_properlyAssignsFields() {
        WeaveViolation weaveViolation = new WeaveViolation(WeaveViolationType.CLASS_WEAVE_IS_INTERFACE, "clazz", "field");

        assertEquals("clazz", weaveViolation.getClazz());
        assertEquals("field", weaveViolation.getField());
        assertEquals(WeaveViolationType.CLASS_WEAVE_IS_INTERFACE, weaveViolation.getType());

        Method method = new Method("name", "desc");
        weaveViolation = new WeaveViolation(WeaveViolationType.CLASS_WEAVE_IS_INTERFACE, "clazz", method);

        assertEquals("clazz", weaveViolation.getClazz());
        assertEquals(method, weaveViolation.getMethod());
        assertEquals(WeaveViolationType.CLASS_WEAVE_IS_INTERFACE, weaveViolation.getType());
    }

    @Test
    public void equalsAndHashCode_evaluateProperly() {
        WeaveViolation weaveViolation1 = new WeaveViolation(WeaveViolationType.CLASS_WEAVE_IS_INTERFACE, "clazz", "field");
        WeaveViolation weaveViolation2 = new WeaveViolation(WeaveViolationType.CLASS_WEAVE_IS_INTERFACE, "clazz", "field");
        WeaveViolation weaveViolation3 = new WeaveViolation(WeaveViolationType.FIELD_TYPE_MISMATCH, "clazz", "field");
        WeaveViolation weaveViolation4 = new WeaveViolation(WeaveViolationType.FIELD_TYPE_MISMATCH, null, "field");
        WeaveViolation weaveViolation5 = new WeaveViolation(WeaveViolationType.FIELD_TYPE_MISMATCH, "clazz");
        WeaveViolation weaveViolation56666666666666666 = new WeaveViolation(WeaveViolationType.FIELD_TYPE_MISMATCH, "clazz");

        assertEquals(weaveViolation1, weaveViolation1);
        assertEquals(weaveViolation1, weaveViolation2);

        assertNotEquals(null, weaveViolation1);

        assertNotEquals(weaveViolation3, weaveViolation1);
        assertNotEquals(weaveViolation4, weaveViolation5);
        assertNotEquals(weaveViolation5, weaveViolation4);

        assertNotEquals(0, weaveViolation1.hashCode());
    }
}
