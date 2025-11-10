/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

/**
 *<p>
 *  This module has no tests because Spring Webclient is an async web client and
 *  the code where data is captured runs in a different thread. To be able to link
 *  the threads, when running the regular agent, other instrumentation modules are
 *  used (netty-reactor/reactor).
 *</p>
 *<p>
 *  This was not the case for Spring Webclient 5.0, where the code did execute in the
 *  same thread, and only later the execution went to a different thread.
 *</p>
 *<p>
 *  There is no simple way to have this other instrumentation module loaded here.
 *  So for now there are no tests in this module.
 *</p>
 *<p>
 *  Once this is fixed, the tests can be copied from the `spring-webclient-5.0` instrumentation
 *  module, since the instrumentation is the same.
 *</p>
 */
