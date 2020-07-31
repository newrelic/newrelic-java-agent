/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension.util;

class XmlParsingMessages {

    protected static final String NO_EXT = "There must be an extension to instrument new methods.\n";
    protected static final String NO_EXT_NAME = "The extension must have a name attribute.\n";
    protected static final String NEG_EXT_VER = " The version number must be a double and must be greater than or equal to 0.\n";
    protected static final String NO_INST_TAG = "In order to provide instrumentation, there must be an instrument tag.\n";
    protected static final String NO_PC_TAGS = "A point cut tag is required to instrument a method.\n";
    protected static final String NO_CLASS_NAME = "A class name, interface name, or super class name needs to be specified for every point cut in the extension {0}";
    protected static final String NO_METHOD = "At least one method must be specified for each point cut in the extension {0}";
    protected static final String NO_METHOD_NAME = "A method name must be specified for each method in the extension {0}";
    protected static final String NO_METHOD_PARAMS = "An error occurred when reading the XML method for method {0} in extension {1}";
    protected static final String GEN_PC_ERROR = "An error occurred reading in a pointcut in extension {0} : {1}";

}
