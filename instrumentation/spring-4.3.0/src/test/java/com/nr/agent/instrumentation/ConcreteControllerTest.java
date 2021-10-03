package com.nr.agent.instrumentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/concrete/controller")
public class ConcreteControllerTest extends AbstractControllerTest {

    @GetMapping(path = "/concrete")
    public String concreteController() {
        return "concreteController";
    }
}
