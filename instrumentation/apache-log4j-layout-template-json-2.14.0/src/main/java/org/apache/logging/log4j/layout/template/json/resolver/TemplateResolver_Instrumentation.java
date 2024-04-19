/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.logging.log4j.layout.template.json.resolver;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

import static com.nr.agent.instrumentation.log4j2.layout.template.json.AgentUtils.writeLinkingMetadata;

@Weave(originalName = "org.apache.logging.log4j.layout.template.json.resolver.TemplateResolver", type = MatchType.Interface)
public class TemplateResolver_Instrumentation<V> {
    public void resolve(V value, JsonWriter jsonWriter) {
        Weaver.callOriginal();
        if (value instanceof LogEvent) {
            writeLinkingMetadata((LogEvent)value, jsonWriter.getStringBuilder());
        }
    }
}
