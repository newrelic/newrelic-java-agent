/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import com.google.common.base.Supplier;
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
import java.util.Map.Entry;

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
            attributes = Multimaps.newListMultimap(new HashMap<String, Collection<Object>>(),
                    new Supplier<List<Object>>() {

                        @Override
                        public List<Object> get() {
                            return new ArrayList<>();
                        }
                    });
        }
        return attributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AnnotationDetails) {
            AnnotationDetails other = (AnnotationDetails) obj;
            if (!desc.equals(other.desc)) {
                return false;
            }
            if ((attributes == null || other.attributes == null) && attributes != other.attributes) {
                return false;
            }
            // if (attributes.size() != other.attributes.size()) {
            // return false;
            // }

            for (Entry<String, Object> entry : attributes.entries()) {
                List<Object> list = other.attributes.get(entry.getKey());
                if (!list.contains(entry.getValue())) {
                    return false;
                }
            }

            return true;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "AnnotationDetails [desc=" + desc + ", attributes=" + attributes + "]";
    }

}
