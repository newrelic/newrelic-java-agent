/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.connect.runtime;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.connect.KafkaConnectMetricsReporter;

@Weave(originalName = "org.apache.kafka.connect.runtime.WorkerTask", type = MatchType.BaseClass)
abstract class WorkerTask_Instrumentation {

    abstract WorkerTask.TaskMetricsGroup taskMetricsGroup();

    public void initialize(TaskConfig taskConfig) {

        WorkerTask.TaskMetricsGroup taskMetricsGroup = taskMetricsGroup();
        if (taskMetricsGroup != null) {
            KafkaConnectMetricsReporter.initialize(taskMetricsGroup.metricGroup());
        }
        Weaver.callOriginal();
    }
}
