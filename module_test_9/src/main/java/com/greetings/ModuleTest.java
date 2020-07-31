package com.greetings;

public class ModuleTest {

    public String simpleCrossModuleTransaction() {
        return "Hello " + World.name();
    }
}
