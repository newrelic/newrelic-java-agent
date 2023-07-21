package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.weave.utils.JarUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

public class ClassLoaderClassTransformerTest {

    ClassLoaderClassTransformer target;

    @Before
    public void setup() {
        target = new ClassLoaderClassTransformer(mock(InstrumentationProxy.class),
                Collections.emptySet(),
                new HashSet<>(Arrays.asList(
                        "java/lang/ClassLoader")));
    }

    @Test
    public void test_newClassMatchVisitor() {
        ClassLoaderClassTransformer target = new ClassLoaderClassTransformer(mock(InstrumentationProxy.class),
                Collections.emptySet(), Collections.emptySet());

        ClassReader reader = mock(ClassReader.class);
        when(reader.getClassName()).thenReturn("org/jboss/modules/NamedClassLoader");
        when(reader.getSuperName()).thenReturn("org/jboss/modules/NamedClassLoader");
        ClassVisitor visitor = mock(ClassVisitor.class);
        InstrumentationContext context = mock(InstrumentationContext.class);

        target.newClassMatchVisitor(mock(ClassLoader.class), ClassLoaderClassTransformerTest.class,
                reader, visitor, context);
        verify(context, times(1)).putMatch(any(), any());
    }

    @Test
    public void test_transform() throws IllegalClassFormatException, IOException {
        byte[] bytes = Utils.readClass(ClassLoader.class).b;

        byte[] result = target.transform(ClassLoaderClassTransformerTest.class.getClassLoader(),
                "java/lang/ClassLoader",
                null,null, bytes);

        Assert.assertNotNull(result);
        Assert.assertFalse(Arrays.equals(bytes, result));
    }

    @Test
    public void test_transformWithViolations() throws IllegalClassFormatException, IOException {
        byte[] bytes = Utils.readClass(ClassLoader.class).b;

        byte[] result = target.transform(null,
                "com/newrelic/notexist",
                null,null, bytes);

        Assert.assertNull(result);
    }
}
