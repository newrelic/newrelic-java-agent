/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.web.reactive.result.method;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//Interfaces/classes used to test various mapping annotation scenarios
public class TestControllerClasses {
    @RequestMapping(value = "/root")
    @RestController
    @Controller
    public static class StandardController {
        @GetMapping("/get")
        public void get() {}
        @PostMapping("/post")
        public void post() {}
        @DeleteMapping("/delete")
        public void delete() {}
        @PutMapping("/put")
        public void put() {}
        @PatchMapping("/patch")
        public void patch() {}
    }

    @RequestMapping(value = "/root")
    @RestController
    @Controller
    public static class StandardControllerWithAllRequestMappings {
        @RequestMapping("/get")
        public void get() {}
        @RequestMapping("/get/{id}")
        public void get2() {}
        @RequestMapping("/post")
        public void post() {}
        @RequestMapping("/delete")
        public void delete() {}
        @RequestMapping("/put")
        public void put() {}
        @RequestMapping("/patch")
        public void patch() {}
    }

    @RequestMapping("/root")
    @RestController
    @Controller
    public interface ControllerInterface {
        @GetMapping("/get")
        void get();
        @PostMapping("/post")
        void post();
        @DeleteMapping("delete")
        void delete();
        @RequestMapping("/req")
        void req();
        @GetMapping("/get/{id}")
        void getParam();
    }

    public static class ControllerClassWithInterface implements ControllerInterface {
        @Override
        public void get() {}
        @Override
        public void post() {}
        @Override
        public void delete() {}
        @Override
        public void req() {}
        @Override
        public void getParam() {}
    }

    @RequestMapping(value = "/root")
    @RestController
    @Controller
    public static abstract class ControllerToExtend {
        @GetMapping("/extend")
        abstract public String extend();
    }

    public static class ControllerExtendingAbstractClass extends ControllerToExtend {
        public String extend() {
            return "extend";
        }
    }

    public static class NoAnnotationController {
        public void get() {}
    }
}
