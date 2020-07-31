/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodMapper {

    /**
     * The key is the method name. The value is the list of parameter descriptors. This is the method descriptor minus
     * the return type.
     */
    private final Map<String, List<String>> methods = new HashMap<>();

    /**
     * 
     * Creates this MethodPointCutMapper.
     */
    public MethodMapper() {
        super();
    }

    /**
     * Clears the mapper.
     */
    public void clear() {
        methods.clear();
    }

    /**
     * Adds the method to the list.
     * 
     * @param name The name of the method.
     * @param descriptors The parameter descriptors. This is the method descriptor minus the return type. Each item in
     *        the list should refence a different method signature.
     */
    public void addMethod(final String name, final List<String> descriptors) {
        List<String> descs = methods.get(name);
        if (descs == null) {
            descs = new ArrayList<>(descriptors);
            methods.put(name, descs);
        } else {
            descs.addAll(descriptors);
        }
    }

    /**
     * Returns true if the descriptor was not present and was therefore added to the mapper.
     * 
     * @param name The name of the method.
     * @param descriptor The method descriptor minus the return type.
     */
    public boolean addIfNotPresent(final String name, final String descriptor) {
        List<String> descs = methods.get(name);
        if (descs == null) {
            descs = new ArrayList<>();
            methods.put(name, descs);
        }

        if (!descs.contains(descriptor)) {
            descs.add(descriptor);
            return true;
        }

        return false;
    }

}
