package com.newrelic.agent.bridge.jfr.events.supportability.token;


import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(TokenLinkEvent.NAME)
@Category({ "New Relic JFR Event", "Supportability", "Tokens" })
@Label("Token Linked")
@Description("Token info")
@StackTrace(true)
public class TokenLinkEvent extends Event {
    static final String NAME = "com.newrelic.TokenLink";

    @Label("Token")
    public String token;

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
