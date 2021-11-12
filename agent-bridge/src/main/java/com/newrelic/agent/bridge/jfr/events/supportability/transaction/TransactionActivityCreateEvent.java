package com.newrelic.agent.bridge.jfr.events.supportability.transaction;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.NEW_RELIC_JFR_EVENT;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.NR_NAMESPACE_PREFIX;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.SUPPORTABILITY;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.TRANSACTION;

@Name(TransactionActivityCreateEvent.NAME)
@Category({ NEW_RELIC_JFR_EVENT, SUPPORTABILITY, TRANSACTION })
@Label("TransactionActivity Created")
@Description("Transaction info")
@StackTrace(true)
public class TransactionActivityCreateEvent extends Event {
    static final String NAME = NR_NAMESPACE_PREFIX + "TransactionActivityCreate";

    @Label("TransactionActivity Object")
    public String transactionActivityObject;

    @Label("Transaction Name")
    public String transactionName;

    @Label("Transaction Object")
    public String transactionObject;

    @Label("Transaction GUID")
    public String transactionGuid;
}
