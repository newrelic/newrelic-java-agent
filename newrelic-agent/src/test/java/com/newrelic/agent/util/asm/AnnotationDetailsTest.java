package com.newrelic.agent.util.asm;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.AnnotationVisitor;

public class AnnotationDetailsTest {
    AnnotationVisitor mockAnnotationVisitor = null;

    @Before
    public void setup() {
        mockAnnotationVisitor = Mockito.mock(AnnotationVisitor.class);
    }

    @Test
    public void getValues_whenAttributesAreNull_returnsEmptyList() {
        AnnotationDetails annotationDetails = new AnnotationDetails(mockAnnotationVisitor, "mock");
        Assert.assertTrue(annotationDetails.getValues("test").isEmpty());
    }

    @Test
    public void getValues_withValidAttributeKey_returnsList() {
        AnnotationDetails annotationDetails = new AnnotationDetails(mockAnnotationVisitor, "mock");
        annotationDetails.visit("test", "foo");
        annotationDetails.visit("test", "foo2");
        annotationDetails.visit("test", "foo3");
        Assert.assertEquals(3, annotationDetails.getValues("test").size());
    }

    @Test
    public void getValue_withInvalidAttributeKey_returnsNull() {
        AnnotationDetails annotationDetails = new AnnotationDetails(mockAnnotationVisitor, "mock");
        Assert.assertNull(annotationDetails.getValue("test"));
    }

    @Test
    public void getValue_withValidAttributeKey_returnsNextValue() {
        AnnotationDetails annotationDetails = new AnnotationDetails(mockAnnotationVisitor, "mock");
        annotationDetails.visit("test", "foo");
        annotationDetails.visit("test", "foo2");
        annotationDetails.visit("test", "foo3");
        Assert.assertEquals("foo", annotationDetails.getValue("test"));
    }

    @Test
    public void equals_withTwoEqualObjects_returnsTrue() {
        AnnotationDetails annotationDetails1 = new AnnotationDetails(mockAnnotationVisitor, "mock");
        AnnotationDetails annotationDetails2 = new AnnotationDetails(mockAnnotationVisitor, "mock");

        Assert.assertEquals(annotationDetails1, annotationDetails2);
    }

    @Test
    public void equals_withTwoNonEqualObjects_returnsFalse() {
        AnnotationDetails annotationDetails1 = new AnnotationDetails(mockAnnotationVisitor, "mock");
        AnnotationDetails annotationDetails2 = new AnnotationDetails(mockAnnotationVisitor, "mock2");

        Assert.assertNotEquals(annotationDetails1, annotationDetails2);
    }

    @Test
    public void hashCode_withTwoEqualObjects_returnsSameValue() {
        AnnotationDetails annotationDetails1 = new AnnotationDetails(mockAnnotationVisitor, "mock");
        AnnotationDetails annotationDetails2 = new AnnotationDetails(mockAnnotationVisitor, "mock");

        Assert.assertEquals(annotationDetails1.hashCode(), annotationDetails2.hashCode());
    }

    @Test
    public void hashCode_withTwoNonEqualObjects_returnsDifferentValues() {
        AnnotationDetails annotationDetails1 = new AnnotationDetails(mockAnnotationVisitor, "mock");
        AnnotationDetails annotationDetails2 = new AnnotationDetails(mockAnnotationVisitor, "mock2");

        Assert.assertNotEquals(annotationDetails1.hashCode(), annotationDetails2.hashCode());
    }
}
