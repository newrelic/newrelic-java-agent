package com.newrelic.agent.bridge.jfr.events.supportability.transaction;


import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(TransactionActivityFinishEvent.NAME)
@Category({ "New Relic JFR Event", "Supportability", "Transaction" })
@Label("TransactionActivity Finish")
@Description("Transaction info")
@StackTrace(true)
public class TransactionActivityFinishEvent extends Event {
    static final String NAME = "com.newrelic.TransactionActivityFinish";

    @Label("TransactionActivity Object")
    public String transactionActivityObject;

    @Label("Transaction Name")
    public String transactionName;

    @Label("Transaction Object")
    public String transactionObject;

    @Label("Transaction GUID")
    public String transactionGuid;
}
