package com.nr.agent.instrumentation.log4j2;

import org.junit.Assert;
import org.junit.Test;

public class AgentUtilTest {

    @Test
    public void testUrlEncoding() {
        final String ENCODED_PIPE = "%7C";
        final String ENCODED_SPACE = "+";
        // The main goal of the encoding is to eliminate | characters from the entity.name as | is used as
        // the BLOB_DELIMITER for separating the agent metadata attributes that are appended to log files
        final String valueToEncode = "|My Application|";
        final String expectedEncodedValue = ENCODED_PIPE + "My" + ENCODED_SPACE + "Application" + ENCODED_PIPE;

        String encodedValue = AgentUtil.urlEncode(valueToEncode);

        Assert.assertEquals(expectedEncodedValue, encodedValue);
    }

}
