package com.newrelic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConnectBackoffPolicyTest {

    @Test
    public void shouldReturnCorrectBackoffInterval() {
        ConnectBackoffPolicy connectBackoffPolicy = new ConnectBackoffPolicy();

        for(int backoffDuration : connectBackoffPolicy.BACKOFF_INTERVAL_IN_SEC) {
            assertEquals(connectBackoffPolicy.nextBackoffDuration(), backoffDuration);
        }
    }

}