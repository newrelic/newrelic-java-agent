package com.newrelic.agent.discovery;

import java.io.IOException;
import java.io.ObjectOutput;

public interface Externalizable {
    void writeExternal(ObjectOutput out) throws IOException;
}
