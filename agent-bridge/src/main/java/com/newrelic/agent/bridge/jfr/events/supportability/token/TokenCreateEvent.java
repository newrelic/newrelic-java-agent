package com.newrelic.agent.bridge.jfr.events.supportability.token;


import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(TokenCreateEvent.NAME)
@Category({ "New Relic JFR Event", "Supportability", "Tokens" })
@Label("Token Created")
@Description("Token info")
@StackTrace(true)
public class TokenCreateEvent extends Event {
    static final String NAME = "com.newrelic.TokenCreate";

    @Label("Token Object")
    public String token;

    @Label("Transaction Name")
    public String transactionName;

    @Label("Transaction Object")
    public String transactionObject;

    @Label("Transaction GUID")
    public String transactionGuid;

    @Label("Location")
    public String location;
}
