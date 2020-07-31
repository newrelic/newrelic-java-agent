package com.greetings;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class Java10ModuleTest {

    @Test
    public void simpleModuleTest() {
        ModuleTest moduleTest = new ModuleTest();
        String result = moduleTest.simpleCrossModuleTransaction();
        assertNotNull(result);
    }

}
