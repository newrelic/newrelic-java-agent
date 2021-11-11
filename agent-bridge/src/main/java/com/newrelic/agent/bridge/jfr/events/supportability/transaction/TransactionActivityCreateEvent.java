package com.newrelic.agent.bridge.jfr.events.supportability.transaction;


import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(TransactionActivityCreateEvent.NAME)
@Category({ "New Relic JFR Event", "Supportability", "Transaction" })
@Label("TransactionActivity Created")
@Description("Transaction info")
@StackTrace(true)
public class TransactionActivityCreateEvent extends Event {
    static final String NAME = "com.newrelic.TransactionActivityCreate";

    @Label("TransactionActivity Object")
    public String transactionActivityObject;

    @Label("Transaction Name")
    public String transactionName;

    @Label("Transaction Object")
    public String transactionObject;

    @Label("Transaction GUID")
    public String transactionGuid;
}
