/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.thrift;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.thrift.protocol.TProtocol;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class ProcessFunction<I, T extends TBase> {

    @NewField
    private static final Pattern PROCESSOR_PATTERN = Pattern.compile("(.*)\\$Processor\\$(.*)");

    @Trace
    public final void process(int seqid, TProtocol iprot, TProtocol oprot, I iface) throws TException {
        String[] txName = classNameToTransactionName(this.getClass().getName());
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "Thrift",
                txName);
        Weaver.callOriginal();
    }

    private static String[] classNameToTransactionName(String className) {

        Matcher processorMatcher = PROCESSOR_PATTERN.matcher(className);
        if (processorMatcher.matches()) {
            return new String[] { processorMatcher.group(1), processorMatcher.group(2) };
        } else {
            return new String[] { className };
        }
    }
}
