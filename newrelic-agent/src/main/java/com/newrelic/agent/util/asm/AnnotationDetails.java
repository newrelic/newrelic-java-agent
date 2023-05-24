/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.AnnotationVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * The details for a class or method annotation.
 */
public class AnnotationDetails extends AnnotationVisitor {

    final String desc;
    private ListMultimap<String, Object> attributes;

    public AnnotationDetails(AnnotationVisitor av, String desc) {
        super(WeaveUtils.ASM_API_LEVEL, av);
        this.desc = desc;
    }

    public List<Object> getValues(String name) {
        if (attributes == null) {
            return Collections.emptyList();
        }
        return attributes.get(name);
    }

    public Object getValue(String name) {
        Collection<Object> values = getValues(name);
        if (values.isEmpty()) {
            return null;
        }
        return values.iterator().next();
    }

    @Override
    public void visit(String name, Object value) {
        getOrCreateAttributes().put(name, value);
        super.visit(name, value);
    }

    Multimap<String, Object> getOrCreateAttributes() {
        if (attributes == null) {
            attributes = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
        }
        return attributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        AnnotationDetails annotationDetails = (AnnotationDetails) obj;
        return Objects.equals(desc, annotationDetails.desc) && Objects.equals(attributes, annotationDetails.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(desc, attributes);
    }

    @Override
    public String toString() {
        return "AnnotationDetails [desc=" + desc + ", attributes=" + attributes + "]";
    }
}
