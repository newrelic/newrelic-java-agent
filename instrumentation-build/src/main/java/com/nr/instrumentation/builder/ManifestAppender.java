package com.nr.instrumentation.builder;

import com.newrelic.weave.weavepackage.CachedWeavePackage;
import com.newrelic.weave.weavepackage.WeavePackage;

import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class ManifestAppender {
    private final Manifest manifest = new Manifest();

    void copyAttributesToManifest(WeavePackage pkg) {
        // The Manifest writer won't write anything without this.
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        putOneAttribute(
                CachedWeavePackage.WEAVE_CLASSES_MANIFEST_ATTRIBUTE_NAME,
                pkg.getMatchTypes().keySet(),
                false);
        putOneAttribute(
                CachedWeavePackage.WEAVE_METHODS_MANIFEST_ATTRIBUTE_NAME,
                pkg.getMethodSignatures(),
                true);
        putOneAttribute(
                CachedWeavePackage.ILLEGAL_CLASSES_MANIFEST_ATTRIBUTE_NAME,
                pkg.getIllegalClasses(),
                false);
        putOneAttribute(
                CachedWeavePackage.REFERENCE_CLASSES_MANIFEST_ATTRIBUTE_NAME,
                pkg.getReferencedClassNames(),
                false);
        putOneAttribute(
                CachedWeavePackage.CLASS_REQUIRED_ANNOTATIONS_MANIFEST_ATTRIBUTE_NAME,
                pkg.getAllRequiredAnnotationClasses(),
                false);
        putOneAttribute(
                CachedWeavePackage.METHOD_REQUIRED_ANNOTATIONS_MANIFEST_ATTRIBUTE_NAME,
                pkg.getAllRequiredMethodAnnotationClasses(),
                false);

    }

    private void putOneAttribute(String key, Set<String> values, boolean doubleQuote) {
        String value = values.stream().sorted()
                .map(str -> doubleQuote ? '"' + str + '"' : str)
                .collect(Collectors.joining(","));

        manifest.getMainAttributes().putValue(key, value);
    }

    Manifest getManifest() {
        return manifest;
    }
}
