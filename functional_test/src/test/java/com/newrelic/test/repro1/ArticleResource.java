/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.test.repro1;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.AbstractResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import rx.Single;
import rx.SingleSubscriber;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

@ComponentScan
@SpringBootApplication
@RestController
public class ArticleResource extends AbstractResource {

    @RequestMapping(value = "/", method = { RequestMethod.GET, RequestMethod.HEAD})
    public Single<String> getArticle(HttpServletResponse response) {
        return Single.create(new Single.OnSubscribe<String>() {
            @Override
            public void call(SingleSubscriber<? super String> singleSubscriber) {
                singleSubscriber.onSuccess("Hi");
            }
        });
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }

}
