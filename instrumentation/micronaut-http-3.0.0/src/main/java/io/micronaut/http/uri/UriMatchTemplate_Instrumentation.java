/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.uri;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Weave(originalName = "io.micronaut.http.filter.ClientFilterChain", type = MatchType.ExactClass)
public abstract class UriMatchTemplate_Instrumentation {
    private final Pattern matchPattern = Weaver.callOriginal();
    @NewField
    private String thisTemplateString = null;

    protected UriMatchTemplate_Instrumentation(CharSequence templateString, Object... parserArguments) {
        thisTemplateString = templateString.toString();
    }

    protected UriMatchTemplate_Instrumentation(CharSequence templateString, List<UriTemplate.PathSegment> segments, Pattern matchPattern,
            List<UriMatchVariable> variables) {
        thisTemplateString = templateString.toString();
    }

    public Optional<UriMatchInfo> match(String uri) {

        Optional<UriMatchInfo> result = Weaver.callOriginal();
        if (result.isPresent() && matchPattern != null) {
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "Routing", thisTemplateString);
        }
        return result;
    }
}
