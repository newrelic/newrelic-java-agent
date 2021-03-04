/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.newrelic.weave.WeaveTestUtils;
import com.newrelic.weave.weavepackage.testclasses.JaxRsAnnotations;
import com.newrelic.weave.weavepackage.testclasses.JaxRsSubresourceAnnotations;
import com.newrelic.weave.weavepackage.testclasses.SpringAnnotations;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WeavePackageAnnotationProcessingTest {
    private static final Set<String> expectedJaxRsAnnotationClasses = new HashSet<>();
    private static final Set<String> expectedJaxRsMethodAnnotationClasses = new HashSet<>();
    private static final Set<String> expectedSpringAnnotationClasses = new HashSet<>();
    private static final Set<String> expectedSpringMethodAnnotationClasses = new HashSet<>();

    private static final String SPRING_ANNOTATIONS_CLASS_NAME = SpringAnnotations.class.getName();
    private static final String JAX_RS_ANNOTATIONS_CLASS_NAME = JaxRsAnnotations.class.getName();
    private static final String JAX_RS_SUBRESOURCE_ANNOTATIONS_CLASS_NAME = JaxRsSubresourceAnnotations.class.getName();

    private static final String SPRING_ANNOTATIONS_CLASS_NODE_NAME = "com/newrelic/weave/weavepackage/testclasses/SpringAnnotations";
    private static final String JAX_RS_ANNOTATIONS_CLASS_NODE_NAME = "com/newrelic/weave/weavepackage/testclasses/JaxRsAnnotations";
    private static final String JAX_RS_SUBRESOURCE_ANNOTATIONS_CLASS_NODE_NAME = "com/newrelic/weave/weavepackage/testclasses/JaxRsSubresourceAnnotations";

    @BeforeClass
    public static void init() {
        // jax-rs class annotations
        expectedJaxRsAnnotationClasses.add("javax.ws.rs.Path");
        // jax-rs method annotations
        expectedJaxRsMethodAnnotationClasses.add("javax.ws.rs.PUT");
        expectedJaxRsMethodAnnotationClasses.add("javax.ws.rs.POST");
        expectedJaxRsMethodAnnotationClasses.add("javax.ws.rs.GET");
        expectedJaxRsMethodAnnotationClasses.add("javax.ws.rs.DELETE");
        expectedJaxRsMethodAnnotationClasses.add("javax.ws.rs.HEAD");
        expectedJaxRsMethodAnnotationClasses.add("javax.ws.rs.OPTIONS");
        expectedJaxRsMethodAnnotationClasses.add("javax.ws.rs.Path");
        expectedJaxRsMethodAnnotationClasses.add("javax.ws.rs.PATCH");

        // Spring class annotations
        expectedSpringAnnotationClasses.add("org.springframework.stereotype.Controller");
        expectedSpringAnnotationClasses.add("org.springframework.web.bind.annotation.RestController");
        // Spring method annotations
        expectedSpringMethodAnnotationClasses.add("org.springframework.web.bind.annotation.RequestMapping");
        expectedSpringMethodAnnotationClasses.add("org.springframework.web.bind.annotation.PatchMapping");
        expectedSpringMethodAnnotationClasses.add("org.springframework.web.bind.annotation.PutMapping");
        expectedSpringMethodAnnotationClasses.add("org.springframework.web.bind.annotation.GetMapping");
        expectedSpringMethodAnnotationClasses.add("org.springframework.web.bind.annotation.PostMapping");
        expectedSpringMethodAnnotationClasses.add("org.springframework.web.bind.annotation.DeleteMapping");
    }

    @Test
    public void testJaxRsAnnotationProcessing() throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(WeaveTestUtils.getClassBytes(JAX_RS_SUBRESOURCE_ANNOTATIONS_CLASS_NAME));
        weaveBytes.add(WeaveTestUtils.getClassBytes(JAX_RS_ANNOTATIONS_CLASS_NAME));
        WeavePackageConfig config = WeavePackageConfig.builder().name("weave_unittest").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        final WeavePackage testPackage = new WeavePackage(config, weaveBytes);

        Assert.assertEquals(expectedJaxRsAnnotationClasses, testPackage.getAllRequiredAnnotationClasses());
        Assert.assertEquals(expectedJaxRsMethodAnnotationClasses, testPackage.getAllRequiredMethodAnnotationClasses());

        final Map<String, ClassNode> allClassAnnotationWeaves = testPackage.getAllClassAnnotationWeaves();
        final Map<String, ClassNode> allMethodAnnotationWeaves = testPackage.getAllMethodAnnotationWeaves();

        // all class annotations should map to the JaxRsAnnotations ClassNode
        for (String annotation : expectedJaxRsAnnotationClasses) {
            String weaveNode = allClassAnnotationWeaves.get(annotation).name;
            Assert.assertEquals(JAX_RS_ANNOTATIONS_CLASS_NODE_NAME, weaveNode);
        }
        // all method annotations should map to the JaxRsSubresourceAnnotations ClassNode, regardless of which JaxRs class bytes were processed first
        for (String annotation : expectedJaxRsMethodAnnotationClasses) {
            String weaveNode = allMethodAnnotationWeaves.get(annotation).name;
            Assert.assertEquals(JAX_RS_SUBRESOURCE_ANNOTATIONS_CLASS_NODE_NAME, weaveNode);
        }
    }

    @Test
    public void testJaxRsAnnotationProcessingOppositeClassByteOrder() throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(WeaveTestUtils.getClassBytes(JAX_RS_ANNOTATIONS_CLASS_NAME));
        weaveBytes.add(WeaveTestUtils.getClassBytes(JAX_RS_SUBRESOURCE_ANNOTATIONS_CLASS_NAME));
        WeavePackageConfig config = WeavePackageConfig.builder().name("weave_unittest").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        final WeavePackage testPackage = new WeavePackage(config, weaveBytes);

        Assert.assertEquals(expectedJaxRsAnnotationClasses, testPackage.getAllRequiredAnnotationClasses());
        Assert.assertEquals(expectedJaxRsMethodAnnotationClasses, testPackage.getAllRequiredMethodAnnotationClasses());

        final Map<String, ClassNode> allClassAnnotationWeaves = testPackage.getAllClassAnnotationWeaves();
        final Map<String, ClassNode> allMethodAnnotationWeaves = testPackage.getAllMethodAnnotationWeaves();

        // all class annotations should map to the JaxRsAnnotations ClassNode
        for (String annotation : expectedJaxRsAnnotationClasses) {
            String weaveNode = allClassAnnotationWeaves.get(annotation).name;
            Assert.assertEquals(JAX_RS_ANNOTATIONS_CLASS_NODE_NAME, weaveNode);
        }
        // all method annotations should map to the JaxRsSubresourceAnnotations ClassNode, regardless of which JaxRs class bytes were processed first
        for (String annotation : expectedJaxRsMethodAnnotationClasses) {
            String weaveNode = allMethodAnnotationWeaves.get(annotation).name;
            Assert.assertEquals(JAX_RS_SUBRESOURCE_ANNOTATIONS_CLASS_NODE_NAME, weaveNode);
        }
    }

    @Test
    public void testSpringAnnotationProcessing() throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(WeaveTestUtils.getClassBytes(SPRING_ANNOTATIONS_CLASS_NAME));
        WeavePackageConfig config = WeavePackageConfig.builder().name("weave_unittest").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        final WeavePackage testPackage = new WeavePackage(config, weaveBytes);

        Assert.assertEquals(expectedSpringAnnotationClasses, testPackage.getAllRequiredAnnotationClasses());
        Assert.assertEquals(expectedSpringMethodAnnotationClasses, testPackage.getAllRequiredMethodAnnotationClasses());

        final Map<String, ClassNode> allClassAnnotationWeaves = testPackage.getAllClassAnnotationWeaves();
        final Map<String, ClassNode> allMethodAnnotationWeaves = testPackage.getAllMethodAnnotationWeaves();

        // all class annotations should map to the SpringAnnotations ClassNode
        for (String annotation : expectedSpringAnnotationClasses) {
            String weaveNode = allClassAnnotationWeaves.get(annotation).name;
            Assert.assertEquals(SPRING_ANNOTATIONS_CLASS_NODE_NAME, weaveNode);
        }
        // all method annotations should map to the SpringAnnotations ClassNode
        for (String annotation : expectedSpringMethodAnnotationClasses) {
            String weaveNode = allMethodAnnotationWeaves.get(annotation).name;
            Assert.assertEquals(SPRING_ANNOTATIONS_CLASS_NODE_NAME, weaveNode);
        }
    }
}
