/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.test.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.Map;

public class ProductHandlers {

    static Map<String, String> products = new HashMap<>();

    static {
        products.put("milk", "3.99");
        products.put("eggs", "2.99");
        products.put("cheese", "2.59");
        products.put("yogurt", "1.59");
        products.put("chocolate", "1.59");
    }

    public static void getAllProducts(RoutingContext context) {
        context.response().putHeader("content-type", "application/json; charset=utf-8").end(
                Json.encodePrettily(products.values()));
    }

    public static Handler<RoutingContext> getProductHandler() {
        return context -> {
            String productName = context.request().getParam("pid");
            if (products.containsKey(productName)) {
                context.response().putHeader("content-type", "application/json; charset=utf-8").end(
                    Json.encodePrettily(products.get(productName)));
            }
            else {
                context.response().setStatusCode(400).end();
            }
        };
    }
}
