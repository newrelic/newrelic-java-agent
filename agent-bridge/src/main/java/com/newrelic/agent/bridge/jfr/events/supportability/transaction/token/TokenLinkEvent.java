package com.newrelic.agent.bridge.jfr.events.supportability.transaction.token;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.NEW_RELIC_JFR_EVENT;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.NR_NAMESPACE_PREFIX;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.SUPPORTABILITY;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.TOKEN;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.TRANSACTION;

@Name(TokenLinkEvent.NAME)
@Category({ NEW_RELIC_JFR_EVENT, SUPPORTABILITY, TRANSACTION, TOKEN })
@Label("Token Linked")
@Description("Token info")
@StackTrace(true)
public class TokenLinkEvent extends Event {
    static final String NAME = NR_NAMESPACE_PREFIX + "TokenLink";

    @Label("Token Object")
    public String tokenObject;

    @Label("Transaction Name")
    public String transactionName;

    @Label("Transaction Object")
    public String transactionObject;

    @Label("Transaction GUID")
    public String transactionGuid;

    @Label("Location")
    public String location;

    @Label("Link Status")
    public String linkStatus;
}
