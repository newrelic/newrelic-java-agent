/*
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 */

/*
 * This script generates the supportability metric names for instrumentation modules.
 * It lists both loaded and skipped metrics and outputs them in order.
 * Its output can replace the metric names in angler, so you don't have to:
 *   1. search for new modules;
 *   2. find the correct line to add them.
 *
 * To run it, in the repo's root folder execute:
 * groovy automation/angler/instrumentation_modules.groovy
 * @author tcrone
 */
def modules = []
new File("settings.gradle").eachLine {line ->
    if(line.contains("include 'instrumentation:")) {
        def i = line.indexOf(":")
        def module = line.substring(i + 1, line.length() - 1)
        modules << module
    }
}

modules.sort()

modules.each {
    println "Supportability/WeaveInstrumentation/Loaded/com.newrelic.instrumentation.$it/1"
}

println ""
println "// Skipped instrumentation modules"

modules.each {
    println "Supportability/WeaveInstrumentation/Skipped/com.newrelic.instrumentation.$it/1"
}
