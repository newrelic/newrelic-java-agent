/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(path = arrayOf("/kotlin"))
class KotlinSpringClass constructor() {

    @RequestMapping(value = ["/read"])
    fun read(data: List<String>, defaultParam: Int = 0): String {
        for (string in data) {
            System.out.println(string)
        }
        return "kotlinDefaultParameter"
    }

}
