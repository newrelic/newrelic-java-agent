package com.nr.agent.instrumentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PlaceHolderPath {

    @RequestMapping("/${request.mapping}")
    public String path(){
        return "path";
    }

}
