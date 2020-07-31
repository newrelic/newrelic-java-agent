/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import com.newrelic.agent.service.Service;

public interface NormalizationService extends Service {

    Normalizer getMetricNormalizer(String appName);

    Normalizer getTransactionNormalizer(String appName);

    Normalizer getUrlNormalizer(String appName);

    String getUrlBeforeParameters(String url);

}
