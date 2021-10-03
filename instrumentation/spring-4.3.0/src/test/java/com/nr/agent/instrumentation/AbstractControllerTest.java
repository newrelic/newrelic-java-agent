package com.nr.agent.instrumentation;

import org.springframework.web.bind.annotation.GetMapping;

public abstract class AbstractControllerTest {

    @GetMapping
    public String concreteController() {
        return "";
    }

    @GetMapping
    public String abstractControllerNoPath() {
        return "abstractControllerNoPath";
    }

    @GetMapping(path = "/abstract")
    public String abstractControllerPath() {
        return "abstractControllerPath";
    }

}
