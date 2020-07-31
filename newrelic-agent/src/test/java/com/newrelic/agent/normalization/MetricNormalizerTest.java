/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigFactoryTest;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.NormalizationRuleConfig;
import com.newrelic.agent.service.ServiceFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricNormalizerTest {

    private static final String APP_NAME = "Unit Test";

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        Map<String, Object> settings = AgentConfigFactoryTest.createStagingMap();
        AgentConfig config = AgentConfigFactory.createAgentConfig(settings, null, null);
        MockConfigService configService = new MockConfigService(null);
        configService.setAgentConfig(config);
        serviceManager.setConfigService(configService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName("Metric Normalizer Test");
        rpmServiceManager.setRPMService(rpmService);

        serviceManager.setNormalizationService(new NormalizationServiceImpl());
    }

    /*
     * @Test public void time() throws Exception { int count = 10000000;
     * 
     * URLNormalizer normalizer = new URLNormalizer(); for (int i = 0; i < count; i++) {
     * normalizer.normalizeURL("/APPLICATION_NAME/test/a/really/long/url/dude?test=true"); } }
     */

    @Test
    public void add() {
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(APP_NAME);
        Assert.assertEquals("/newrelic-solr-webapp/add", urlNormalizer.normalize("/newrelic-solr-webapp/add"));
    }

    @Test
    public void noForwardSlash() {
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(APP_NAME);
        Assert.assertEquals("/newrelic-solr-webapp/add", urlNormalizer.normalize("newrelic-solr-webapp/add"));
    }

    @Test
    public void uglyugly() {
        NormalizationService service = ServiceFactory.getNormalizationService();
        Assert.assertEquals(
                "/v",
                service.getUrlNormalizer(APP_NAME).normalize(
                        service.getUrlBeforeParameters("v?,\006����?&\b-��\020\bI?$\004????��\022?*??NO��RUV?������??��??*��????u?\005.?P{m\0")));
    }

    private String setUrlRules(JSONArray rules) {
        MockRPMServiceManager rpmServiceManager = (MockRPMServiceManager) ServiceFactory.getRPMServiceManager();
        List<ConnectionListener> connectionListeners = rpmServiceManager.getConnectionListeners();
        ConnectionListener connectionListener = connectionListeners.get(0);
        MockRPMService rpmService = (MockRPMService) rpmServiceManager.getRPMService();
        String appName = rpmService.getApplicationName();
        Map<String, Object> data = new HashMap<>();
        data.put(NormalizationRuleConfig.URL_RULES_KEY, rules);
        // add a bogus segment term rule to make sure that has no side-effect of breaking stuff
        data.put("transaction_segment_terms", Arrays.asList(ImmutableMap.of("prefix", "This/Wont/Match/Anything",
                "terms", Arrays.asList("of", "endearment"))));
        connectionListener.connected(rpmService, AgentConfigImpl.createAgentConfig(data));
        return appName;
    }

    @Test
    public void resourceNormalizationRule() {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "(.*)/[^/]*.(bmp|css|gif|ico|jpg|jpeg|js|png)$");
                put("replacement", "\\1/*.\\2");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
                put("each_segment", Boolean.FALSE);
            }
        }));
        String appName = setUrlRules(rules);
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(appName);
        Assert.assertEquals("/test/dude/*.jpg", urlNormalizer.normalize("/test/dude/flower.jpg"));
        Assert.assertEquals("/*.ICO", urlNormalizer.normalize("/DUDE.ICO"));
    }

    @Test
    public void hexadecimalEachSegmentRule() {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^[0-9a-f]*[0-9][0-9a-f]*$");
                put("replacement", "*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
                put("each_segment", Boolean.TRUE);
            }
        }));
        String appName = setUrlRules(rules);
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(appName);
        Assert.assertEquals("/test/1axxx/*/*/bad/*/*/x999/*",
                urlNormalizer.normalize("/test/1axxx/4babe/cafe222/bad/a1b2c3d3e4f5/ABC123/x999/111"));
        Assert.assertEquals("/test/*/dude", urlNormalizer.normalize("/test/4/dude"));
        Assert.assertEquals("/test/*/999x", urlNormalizer.normalize("/test/babe4/999x"));
        Assert.assertEquals("/geteclipse/resource/themes/images/*",
                urlNormalizer.normalize("/geteclipse/resource/themes/images/9ae0f28"));
        Assert.assertEquals("/test/*/dude.jsp", urlNormalizer.normalize("/test/4/dude.jsp"));
        Assert.assertEquals("/geteclipse/resource/themes/images/add",
                urlNormalizer.normalize("/geteclipse/resource/themes/images/add"));
    }

    @Test
    public void removeAllTicksRule() {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "([^']*)'+");
                put("replacement", "\\1");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
                put("each_segment", Boolean.FALSE);
                put("replace_all", Boolean.TRUE);
            }
        }));
        String appName = setUrlRules(rules);
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(appName);
        Assert.assertEquals("/test//bad/a1b2c3d3e4f5/x999/111",
                urlNormalizer.normalize("/test/'''/b'a''d''/a1b2'c3'd3e4f5/x999/111'"));
    }

    @Test
    public void UrlEncodedSegmentsRule() {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "(.*)%(.*)");
                put("replacement", "*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
                put("each_segment", Boolean.TRUE);
                put("terminate_chain", Boolean.FALSE);
                put("replace_all", Boolean.FALSE);
            }
        }));
        String appName = setUrlRules(rules);
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(appName);
        Assert.assertEquals("/test/*/*/*/x999/*", urlNormalizer.normalize("/test/%%%/bad%%/a1b2%c3%d3e4f5/x999/111%"));
        Assert.assertEquals("/add-resource/*",
                urlNormalizer.normalize("/add-resource/aHR0cDovL2RicGVkaWEub3JnL3Jlc291cmNlL1RpbV9Kb25lcw%3D%3D"));
    }

    @Test
    public void chainedRules() {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^[0-9a-f]*[0-9][0-9a-f]*$");
                put("replacement", "*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
                put("each_segment", Boolean.TRUE);
                put("terminate_chain", Boolean.FALSE);
            }
        }, new JSONObject() {
            {
                put("match_expression", "(.*)/fritz/(.*)");
                put("replacement", "\\1/karl/\\2");
                put("ignore", Boolean.FALSE);
                put("eval_order", 11);
                put("each_segment", Boolean.FALSE);
                put("terminate_chain", Boolean.TRUE);
            }
        }));
        String appName = setUrlRules(rules);
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(appName);
        Assert.assertEquals("/test/1axxx/*/karl/x999/*", urlNormalizer.normalize("/test/1axxx/4babe/fritz/x999/111"));
    }

    @Test
    public void ignoreRule() {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^/artists/az/(.*)/(.*)$");
                put("replacement", "/artists/az/*/\\2");
                put("ignore", Boolean.TRUE);
                put("eval_order", 11);
            }
        }));
        String appName = setUrlRules(rules);
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(appName);
        Assert.assertNull(urlNormalizer.normalize("/artists/az/angelfish/artist.jhtml"));
    }

    @Test
    public void ignoreRuleEachSegment() {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^[0-9a-f]*[0-9][0-9a-f]*$");
                put("ignore", Boolean.TRUE);
                put("eval_order", 11);
                put("each_segment", Boolean.TRUE);
            }
        }));
        String appName = setUrlRules(rules);
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(appName);
        Assert.assertNull(urlNormalizer.normalize("/test/1axxx/4babe/999x"));
    }

    @Test
    public void ignoreRuleEachSegmentNoMatch() {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^[0-9a-f]*[0-9][0-9a-f]*$");
                put("ignore", Boolean.TRUE);
                put("eval_order", 11);
                put("each_segment", Boolean.TRUE);
            }
        }));
        String appName = setUrlRules(rules);
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(appName);
        Assert.assertEquals("/test/1axxx/4ubabe/999x", urlNormalizer.normalize("/test/1axxx/4ubabe/999x"));
    }

    @Test
    public void stripsParams() {
        Assert.assertEquals("/test/foo.do", ServiceFactory.getNormalizationService().getUrlBeforeParameters(
                "/test/foo.do;jsessionid=934885774"));
        Assert.assertEquals("/test/foo.do", ServiceFactory.getNormalizationService().getUrlBeforeParameters(
                "/test/foo.do?filter=true"));
        Assert.assertEquals("/test/foo.do", ServiceFactory.getNormalizationService().getUrlBeforeParameters(
                "/test/foo.do?filter=true#anchor"));
        Assert.assertEquals("/test/foo.do", ServiceFactory.getNormalizationService().getUrlBeforeParameters(
                "/test/foo.do#someanchor"));
    }

    @Test
    public void whatUpDog() {
        Assert.assertEquals("/what", ServiceFactory.getNormalizationService().getUrlBeforeParameters("/what?up=dog"));
    }

    @Test
    public void numberRule() {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "\\d+");
                put("replacement", "*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
                put("each_segment", Boolean.FALSE);
                put("replace_all", Boolean.TRUE);
            }
        }));
        String appName = setUrlRules(rules);
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(appName);
        Assert.assertEquals("/solr/shard*/select", urlNormalizer.normalize("/solr/shard03/select"));
        Assert.assertEquals("/hey/r*d*", urlNormalizer.normalize("/hey/r2d2"));
    }

    @Test
    public void numberRuleEachSegment() {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "\\d+");
                put("replacement", "*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
                put("each_segment", Boolean.TRUE);
                put("replace_all", Boolean.TRUE);
            }
        }));
        String appName = setUrlRules(rules);
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(appName);
        Assert.assertEquals("/solr/shard*/select", urlNormalizer.normalize("/solr/shard03/select"));
        Assert.assertEquals("/hey/r*d*", urlNormalizer.normalize("/hey/r2d2"));
    }

    @Test
    public void testCustomRule() {
        JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^/([^/]*=[^/]*&?)+");
                put("replacement", "/all_params");
                put("ignore", Boolean.FALSE);
                put("eval_order", 0);
                put("each_segment", Boolean.FALSE);
            }
        }, new JSONObject() {
            {
                put("match_expression", "^/.*/PARAMS/(article|legacy_article|post|product)/.*");
                put("replacement", "/*/PARAMS/\\1/*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 14);
            }
        }, new JSONObject() {
            {
                put("match_expression", "^/test/(.*)");
                put("replacement", "/dude");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
            }
        }, new JSONObject() {
            {
                put("match_expression", "^/blah/(.*)");
                put("replacement", "/\\1");
                put("ignore", Boolean.FALSE);
                put("eval_order", 2);
            }
        }, new JSONObject() {
            {
                put("match_expression", "/.*(dude|man)");
                put("replacement", "/*.\\1");
                put("ignore", Boolean.FALSE);
                put("eval_order", 3);
            }
        }, new JSONObject() {
            {
                put("match_expression", "/store/\\$\\*c/.*");
                put("replacement", "/store/\\$\\*c/*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 4);
            }
        }, new JSONObject() {
            {
                put("match_expression", "^/(bob)");
                put("replacement", "/\\1ert/\\1/\\1ertson");
                put("ignore", Boolean.FALSE);
                put("eval_order", 4);
                put("each_segment", Boolean.FALSE);
            }
        }, new JSONObject() {
            {
                put("match_expression", "/foo(.*)");
                put("ignore", Boolean.TRUE);
                put("eval_order", 5);
                put("each_segment", Boolean.FALSE);
            }
        }));
        List<NormalizationRule> urlRules = NormalizationRuleFactory.getUrlRules("Test", rulesData);
        Normalizer normalizer = NormalizerFactory.createUrlNormalizer("Test", urlRules);

        Assert.assertEquals(
                "/all_params",
                normalizer.normalize("/dechannel=igncodes&reggender=*&regcountry=US&property=ign&regage=*&platform_id=*&"));
        Assert.assertEquals("/*/PARAMS/article/*", normalizer.normalize("/*on*-working-from-home/PARAMS/article/*"));
        Assert.assertEquals("/bobert/bob/bobertson", normalizer.normalize("/bob"));
        Assert.assertEquals("/store/$*c/*",
                normalizer.normalize("/store/$*c/A-Tribe-Called-Quest/LT_talent=*/Beats-Rhymes-Life/*/LT_sku=*"));
        Assert.assertEquals("/dude", normalizer.normalize("/test/foobar"));
        Assert.assertEquals("/bar/test", normalizer.normalize("/bar/test"));
        Assert.assertEquals("/test/man", normalizer.normalize("/blah/test/man"));

        Assert.assertEquals("/*.dude", normalizer.normalize("/oh/hey.dude"));
        Assert.assertEquals("/*.man", normalizer.normalize("/oh/hey/what/up.man"));

        Assert.assertNull(normalizer.normalize("/foo"));
        Assert.assertNull(normalizer.normalize("/foo/foobar"));
    }

    @Test
    public void testCustomRuleOrder() {
        final String matchOnTest = "/test/(.*)";
        JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", matchOnTest);
                put("replacement", "/el_duderino");
                put("ignore", Boolean.FALSE);
                put("eval_order", 37);
            }
        }, new JSONObject() {
            {
                put("match_expression", matchOnTest);
                put("replacement", "/dude");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
            }
        }, new JSONObject() {
            {
                put("match_expression", "/blah/(.*)");
                put("replacement", "/$1");
                put("ignore", Boolean.FALSE);
                put("eval_order", 2);
            }
        }, new JSONObject() {
            {
                put("match_expression", "/foo(.*)");
                put("ignore", Boolean.TRUE);
                put("eval_order", 3);
            }
        }));
        List<NormalizationRule> urlRules = NormalizationRuleFactory.getUrlRules("Test", rulesData);
        Normalizer normalizer = NormalizerFactory.createUrlNormalizer("Test", urlRules);

        // Two rules match, but only one is applied due to ordering
        Assert.assertEquals("/dude", normalizer.normalize("/test/foobar"));

        rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "/test/(.*)");
                put("replacement", "/you_first");
                put("ignore", Boolean.FALSE);
                put("eval_order", 0);
            }
        }, new JSONObject() {
            {
                put("match_expression", "/test/(.*)");
                put("replacement", "/no_you");
                put("ignore", Boolean.FALSE);
                put("eval_order", 0);
            }
        }, new JSONObject() {
            {
                put("match_expression", "/test/(.*)");
                put("replacement", "/please_after_you");
                put("ignore", Boolean.FALSE);
                put("eval_order", 0);
            }
        }));
        urlRules = NormalizationRuleFactory.getUrlRules("Test", rulesData);
        normalizer = NormalizerFactory.createUrlNormalizer("Test", urlRules);

        // Stable sorting done in setUrlRules
        Assert.assertEquals("/you_first", normalizer.normalize("/test/polite_seattle_drivers"));
    }

    @Test
    public void testCustomRuleChaining() {
        JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "(.*)/robertson(.*)");
                put("replacement", "\\1/LAST_NAME\\2");
                put("ignore", Boolean.FALSE);
                put("eval_order", 0);
                put("terminate_chain", false);
            }
        }, new JSONObject() {
            {
                put("match_expression", "^/robert(.*)");
                put("replacement", "/bob\\1");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
                // stops chain
            }
        }, new JSONObject() {
            {
                put("match_expression", "/LAST_NAME");
                put("replacement", "/fail");
                put("ignore", Boolean.FALSE);
                put("eval_order", 2);
            }
        }));
        List<NormalizationRule> urlRules = NormalizationRuleFactory.getUrlRules("Test", rulesData);
        Normalizer normalizer = NormalizerFactory.createUrlNormalizer("Test", urlRules);

        Assert.assertEquals("/bob/LAST_NAME", normalizer.normalize("/robert/robertson"));
    }

    @Test
    public void testCustomRuleUnmatchedGroup() {
        JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "(.*)");
                put("replacement", "\\1");
                put("ignore", Boolean.FALSE);
                put("eval_order", 0);
            }
        }));
        List<NormalizationRule> urlRules = NormalizationRuleFactory.getUrlRules("Test", rulesData);
        Normalizer normalizer = NormalizerFactory.createUrlNormalizer("Test", urlRules);

        // Don't really care what the unmatched group is replaced with, as long as it doesn't blow up
        Assert.assertEquals("/the_whole_path", normalizer.normalize("/the_whole_path"));
    }

    @Test
    public void testCustomRuleNoPartialMatches() {
        JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^/userid/.*/folderid");
                put("replacement", "/userid/*/folderid/*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
            }
        }, new JSONObject() {
            {
                put("match_expression", "/need_not_be_first_segment/.*");
                put("replacement", "*/need_not_be_first_segment/*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 2);
            }
        }));
        List<NormalizationRule> urlRules = NormalizationRuleFactory.getUrlRules("Test", rulesData);
        Normalizer normalizer = NormalizerFactory.createUrlNormalizer("Test", urlRules);

        // Decimal numbers get collapsed, but the custom rule doesn't match
        Assert.assertEquals("/userid/*/folderid/*/qwerty7356",
                normalizer.normalize("/userid/3434asdf/folderid/qwerty7356"));

        // Partial match doesn't work here, either
        Assert.assertEquals("/first*/need_not_be_first_segment/*",
                normalizer.normalize("/first/need_not_be_first_segment/uiop"));
    }

    // @Test
    public void performance() {
        JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^/org/apache/catalina/startup/Bootstrap");
                put("replacement", "/org/apache/catalina/startup/Bootstrap");
                put("eval_order", 1);
                put("ignore", Boolean.FALSE);
                put("each_segment", Boolean.FALSE);
            }
        }));
        String appName = setUrlRules(rulesData);
        Normalizer urlNormalizer = ServiceFactory.getNormalizationService().getUrlNormalizer(appName);
        Assert.assertEquals("/org/apache/catalina/startup/Bootstrap",
                urlNormalizer.normalize("/org/apache/catalina/startup/Bootstrap"));

        long startTime = System.currentTimeMillis();
        int count = 100000;
        for (int i = 0; i < count; i++) {
            ServiceFactory.getNormalizationService().getUrlNormalizer(appName).normalize(
                    "/artists/archive/t/twain.jhtml");
        }
        long stopTime = System.currentTimeMillis();
        String msg = MessageFormat.format("{0} iterations took {1} milliseconds ({2} nanoseconds per iteration)",
                count, stopTime - startTime, (stopTime - startTime) * 1000 / count);
        System.out.println(msg);
    }

}
