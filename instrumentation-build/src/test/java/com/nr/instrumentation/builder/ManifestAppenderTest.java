package com.nr.instrumentation.builder;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.weave.weavepackage.CachedWeavePackage;
import com.newrelic.weave.weavepackage.WeavePackage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.Attributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManifestAppenderTest {
    @Test
    public void appendsValues() throws IOException {
        WeavePackage mockPackage = mock(WeavePackage.class);
        Map<Attributes.Name, String> expectedValues = new HashMap<>();

        when(mockPackage.getAllRequiredAnnotationClasses()).thenReturn(new HashSet<>(Arrays.asList("reqann1", "reqann2")));
        expectedValues.put(new Attributes.Name(CachedWeavePackage.CLASS_REQUIRED_ANNOTATIONS_MANIFEST_ATTRIBUTE_NAME), "reqann1,reqann2");

        when(mockPackage.getAllRequiredMethodAnnotationClasses()).thenReturn(new HashSet<>(Arrays.asList("reqmeth1", "reqmeth2")));
        expectedValues.put(new Attributes.Name(CachedWeavePackage.METHOD_REQUIRED_ANNOTATIONS_MANIFEST_ATTRIBUTE_NAME), "reqmeth1,reqmeth2");

        when(mockPackage.getIllegalClasses()).thenReturn(Collections.singleton("skipme"));
        expectedValues.put(new Attributes.Name(CachedWeavePackage.ILLEGAL_CLASSES_MANIFEST_ATTRIBUTE_NAME), "skipme");

        when(mockPackage.getMatchTypes()).thenReturn(Collections.singletonMap("matchtypek", MatchType.BaseClass));
        expectedValues.put(new Attributes.Name(CachedWeavePackage.WEAVE_CLASSES_MANIFEST_ATTRIBUTE_NAME), "matchtypek");

        when(mockPackage.getMethodSignatures()).thenReturn(new HashSet<>(Arrays.asList("zzzsignature", "aaasignature")));
        expectedValues.put(new Attributes.Name(CachedWeavePackage.WEAVE_METHODS_MANIFEST_ATTRIBUTE_NAME), "\"aaasignature\",\"zzzsignature\"");

        when(mockPackage.getReferencedClassNames()).thenReturn(Collections.emptySet());
        expectedValues.put(new Attributes.Name(CachedWeavePackage.REFERENCE_CLASSES_MANIFEST_ATTRIBUTE_NAME), "");

        expectedValues.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        ManifestAppender target = new ManifestAppender();
        target.copyAttributesToManifest(mockPackage);

        assertEquals(expectedValues, target.getManifest().getMainAttributes());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        target.getManifest().write(baos);

        String manifest = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        for (Map.Entry<Attributes.Name, String> expectedEntry : expectedValues.entrySet()) {
            assertTrue(manifest.contains(expectedEntry.getKey().toString() + ": " + expectedEntry.getValue() + "\r\n"));
        }
    }
}