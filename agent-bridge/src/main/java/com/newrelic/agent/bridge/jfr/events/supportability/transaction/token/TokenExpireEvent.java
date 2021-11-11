package com.newrelic.agent.bridge.jfr.events.supportability.transaction.token;


import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(TokenExpireEvent.NAME)
@Category({ "New Relic JFR Event", "Supportability", "Transaction", "Token" })
@Label("Token Expired")
@Description("Token info")
@StackTrace(true)
public class TokenExpireEvent extends Event {
    static final String NAME = "com.newrelic.TokenExpire";

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
}
