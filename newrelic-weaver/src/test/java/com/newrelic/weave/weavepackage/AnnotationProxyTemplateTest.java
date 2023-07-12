package com.newrelic.weave.weavepackage;

import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class AnnotationProxyTemplateTest {
    @Test
    public void getOrCreateAnnotationHolder_onInitialCall_returnsCorrectAnnotationProxy() {
        Class<Annotation> annotationClass = Annotation.class;

        Annotation proxy = AnnotationProxyTemplate.getOrCreateAnnotationHolder(Annotation.class, annotationClass, "val1", "val2");
        assertNotNull(proxy);
    }

    @Test
    public void getOrCreateAnnotationHolder_onSubsequentCall_returnsFirstCallProxy() {
        Class<Annotation> annotationClass = Annotation.class;

        Annotation proxy1 = AnnotationProxyTemplate.getOrCreateAnnotationHolder(Annotation.class, annotationClass, "val1", "val2");
        Annotation proxy2 = AnnotationProxyTemplate.getOrCreateAnnotationHolder(Annotation.class, annotationClass, "val1", "val2");
        assertNotNull(proxy2);
        assertSame(proxy1, proxy2);
    }
}
