/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.config.SystemPropertyProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KubernetesData {

    static final String KUBERNETES_SERVICE_HOST_ENV = "KUBERNETES_SERVICE_HOST";
    static final String KUBERNETES_SERVICE_HOST_KEY = "kubernetes_service_host";

    static final KubernetesData EMPTY_KUBERNETES_DATA = new KubernetesData(Collections.<String, String>emptyMap());

    private final Map<String, String> kubernetesData;

    private KubernetesData(Map<String, String> kubernetesData) {
        this.kubernetesData = kubernetesData;
    }

    static KubernetesData extractKubernetesValues(SystemPropertyProvider systemPropertyProvider) {
        Map<String, String> kubernetesData = new HashMap<>();

        String kubernetesServiceHost = systemPropertyProvider.getEnvironmentVariable(KUBERNETES_SERVICE_HOST_ENV);
        if (kubernetesServiceHost != null) {
            kubernetesData.put(KUBERNETES_SERVICE_HOST_KEY, kubernetesServiceHost);
        }

        return new KubernetesData(kubernetesData);
    }

    public Map<String, String> getValueMap() {
        return kubernetesData;
    }

}
