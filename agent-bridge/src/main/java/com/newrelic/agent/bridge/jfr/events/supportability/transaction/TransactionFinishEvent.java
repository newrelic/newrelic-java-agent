package com.newrelic.agent.bridge.jfr.events.supportability.transaction;


import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(TransactionFinishEvent.NAME)
@Category({ "New Relic JFR Event", "Supportability", "Transaction" })
@Label("Transaction Finished")
@Description("Transaction info")
@StackTrace(true)
public class TransactionFinishEvent extends Event {
    static final String NAME = "com.newrelic.TransactionFinish";

    @Label("Transaction Name")
    public String transactionName;

    @Label("Transaction Object")
    public String transactionObject;

    @Label("Transaction GUID")
    public String transactionGuid;
}
