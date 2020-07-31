package com.newrelic;

import com.newrelic.api.agent.Logger;

import java.util.logging.Level;

public class LoopForever implements Runnable {
    private final Logger logger;
    private final Runnable spanDeliveryConsumer;

    public LoopForever(
            Logger logger,
            Runnable spanDeliveryConsumer) {
        this.logger = logger;
        this.spanDeliveryConsumer = spanDeliveryConsumer;
    }

    @Override
    public void run() {
        logger.log(Level.FINE, "Initializing {0}", this);
        while (true) {
            try {
                spanDeliveryConsumer.run();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, t, "There was a problem with the Infinite Tracing span sender, and no further spans will be sent");
                return;
            }
        }
    }
}
