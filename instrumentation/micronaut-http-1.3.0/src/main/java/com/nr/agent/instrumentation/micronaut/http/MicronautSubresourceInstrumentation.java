/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut.http;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import com.newrelic.api.agent.weaver.WeaveWithAnnotation;
import com.newrelic.api.agent.weaver.Weaver;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Head;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.Trace;

public class MicronautSubresourceInstrumentation {

    @WeaveWithAnnotation(annotationClasses = { "io.micronaut.http.annotation.Get", "io.micronaut.http.annotation.Post", "io.micronaut.http.annotation.Delete",
            "io.micronaut.http.annotation.Put", "io.micronaut.http.annotation.Head", "io.micronaut.http.annotation.Trace",
            "io.micronaut.http.annotation.Patch" })
    @WeaveIntoAllMethods
    @Trace
    private static void instrumentation() {
        Controller controller = Weaver.getClassAnnotation(Controller.class);
        if (controller != null) {
            String controllerValue = controller.value();
            String methodName = "Unknown";
            Get get = Weaver.getMethodAnnotation(Get.class);

            String value = null;

            if (get != null) {
                methodName = "GET";
                value = get.value();
                if (value == null) {
                    value = get.uri();
                    if (value == null) {
                        String[] values = get.uris();
                        if (values != null) {
                            value = String.join(",", values);
                        }
                    }
                }
            } else {
                Post post = Weaver.getMethodAnnotation(Post.class);
                if (post != null) {
                    methodName = "POST";
                    value = post.value();
                    if (value == null) {
                        value = post.uri();
                        if (value == null) {
                            String[] values = post.uris();
                            if (values != null) {
                                value = String.join(",", values);
                            }
                        }
                    }
                } else {
                    Put put = Weaver.getMethodAnnotation(Put.class);
                    if (put != null) {
                        methodName = "PUT";
                        value = put.value();
                        if (value == null) {
                            value = put.uri();
                            if (value == null) {
                                String[] values = put.uris();
                                if (values != null) {
                                    value = String.join(",", values);
                                }
                            }
                        }
                    } else {
                        Delete delete = Weaver.getMethodAnnotation(Delete.class);
                        if (delete != null) {
                            methodName = "DELETE";
                            value = delete.value();
                            if (value == null) {
                                value = delete.uri();
                                if (value == null) {
                                    String[] values = delete.uris();
                                    if (values != null) {
                                        value = String.join(",", values);
                                    }
                                }
                            }
                        } else {
                            Patch patch = Weaver.getMethodAnnotation(Patch.class);
                            if (patch != null) {
                                methodName = "PATCH";
                                value = patch.value();
                                if (value == null) {
                                    value = patch.uri();
                                    if (value == null) {
                                        String[] values = patch.uris();
                                        if (values != null) {
                                            value = String.join(",", values);
                                        }
                                    }
                                }
                            } else {
                                Head head = Weaver.getMethodAnnotation(Head.class);
                                if (head != null) {
                                    methodName = "HEAD";
                                    value = head.value();
                                    if (value == null) {
                                        String[] values = head.uris();
                                        if (values != null) {
                                            value = String.join(",", values);
                                        }
                                    }
                                } else {
                                    io.micronaut.http.annotation.Trace trace = Weaver.getMethodAnnotation(io.micronaut.http.annotation.Trace.class);
                                    if (trace != null) {
                                        methodName = "TRACE";
                                        value = trace.value();
                                        if (value == null) {
                                            String[] values = trace.uris();
                                            if (values != null) {
                                                value = String.join(",", values);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            StringBuilder sb = new StringBuilder();

            if (controllerValue != null) {
                sb.append(controllerValue);
                sb.append('/');
            }

            if (value != null) {
                sb.append(value);
            }

            sb.append(" (").append(methodName).append(") ");
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "MicronautController", sb.toString());
        }
    }
}
