/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.threads;

import org.junit.Assert;
import org.junit.Test;

public class ThreadNameNormalizerTest {
    private ThreadNameNormalizer normalizer = getThreadNameNormalizer();
    
    public static final ThreadNameNormalizer getThreadNameNormalizer() {
        ThreadNames threadNames = new ThreadNames() {
            
            @Override
            public String getThreadName(BasicThreadInfo thread) {
                return thread.getName();
            }
        };
        return new ThreadNameNormalizer(threadNames);
    }
    
    @Test
    public void hexInServerThreadName() {
        Assert.assertEquals("Server-#-#-selector-ServerConnectorManager@#", 
                            normalizer.getNormalizedThreadName("Server-8091-130-selector-ServerConnectorManager@6182e1ea/7"));
    }
    @Test
    public void underlineDashWordbreaks() {
        Assert.assertEquals("ThreadInfo-timeslices-#-#_hello-one-two-#c.example.com-#-#_watcher_executor",
                            normalizer.getNormalizedThreadName("ThreadInfo/timeslices-84a3c-ae01b_hello-one-two-3c.example.com-3-74ae8_watcher_executor"));
        
    }
    @Test
    public void dontMatchShorterThan3Characters() {
        // won't match short hex numbers
        Assert.assertEquals("caf",normalizer.getNormalizedThreadName("caf"));
        Assert.assertEquals("#",normalizer.getNormalizedThreadName("cafe"));
        Assert.assertEquals("caf-bab",normalizer.getNormalizedThreadName("caf-bab"));
        Assert.assertEquals("caf-#",normalizer.getNormalizedThreadName("caf-babe"));
        Assert.assertEquals("#",normalizer.getNormalizedThreadName("cafebabe"));        
    }
    
    @Test
    public void removeSlashes() {
        // Make sure slashes are removed
        Assert.assertEquals("foo@#", normalizer.getNormalizedThreadName("foo@bar/bat"));
    }
    
    @Test
    public void simpleNumbers() {
        Assert.assertEquals("test-pool-#-#", normalizer.getNormalizedThreadName("test-pool-45-2"));
    }
    
    @Test
    public void objectHex() {
        Assert.assertEquals("net.sf.ehcache.CacheManager@#", normalizer.getNormalizedThreadName("net.sf.ehcache.CacheManager@b12bdc0"));
        //Assert.assertEquals("org.eclipse.jetty.server.session.HashSessionManager@#Timer", normalizer.getNormalizedThreadName("org.eclipse.jetty.server.session.HashSessionManager@2dbcae3Timer"));
    }
    
    @Test
    public void testWordBreaks()  {
        
        // Recognize hex digits between word breaks but don't substitute word breaks
        Assert.assertEquals("timeslice-#-caf",normalizer.getNormalizedThreadName("timeslice-a87b34f0-caf"));
        Assert.assertEquals("timeslice-#-#",normalizer.getNormalizedThreadName("timeslice-a87b34f0-cafe"));
        Assert.assertEquals("timeslice#(#)caf",normalizer.getNormalizedThreadName("timeslice8(a87b34f0)caf"));
        Assert.assertEquals("timeslice#(#)caf",normalizer.getNormalizedThreadName("timeslice8(387b34f0)caf"));
        Assert.assertEquals("timeslices_prod-collector.example.com-#-#_watcher_executor", 
                normalizer.getNormalizedThreadName("timeslices_prod-collector.example.com-1404332009454-ba043e1b_watcher_executor"));

    }
    
    @Test
    public void httpVerbGet() {
        Assert.assertEquals("WebRequest#", 
                normalizer.getNormalizedThreadName("Threads/Time/CPU/dw-# - -flurry-zendesk-requester-email-alice@example.com? - GET -flurry-zendesk-requester-email-alice@example.com?/SystemTime"));
    }
    
    @Test
    public void httpVerbGetNegative() {
        Assert.assertEquals("GettingTired",  normalizer.getNormalizedThreadName("GettingTired"));
    }
    
    @Test
    public void httpVerbPut() {
        Assert.assertEquals("WebRequest#", 
                normalizer.getNormalizedThreadName("dw-# - PUT -v#-corrections-assertions-email-alice%#example.com?minScore=#&apiKey=#"));
    }
    
    @Test
    public void httpVerbPost() {
        Assert.assertEquals("WebRequest#", 
                normalizer.getNormalizedThreadName("dw-# - POST -v#-corrections-assertions-email-alice%#example.com?minScore=#&apiKey=#"));
    }
    
    @Test
    public void httpVerbDelete() {
        Assert.assertEquals("WebRequest#", 
                normalizer.getNormalizedThreadName("dw-# - DELETE -v#-corrections-assertions-email-alice%#example.com?minScore=#&apiKey=#"));
    }
    
    @Test
    public void httpVerbHead() {
        Assert.assertEquals("WebRequest#", 
                normalizer.getNormalizedThreadName("#.#.#.# [#] HEAD -content-careers-twitter-en.html HTTP-#.#"));
    }
    
    @Test
    public void sendingMailitem() {
        Assert.assertEquals("Sending mailitem#", 
                normalizer.getNormalizedThreadName("Sending mailitem To='dude@company.com' Subject='Updated: (EFEF-#) TCM Sep # onwards XSM ID #' From='null' FromName='Some Person (MY JIRA)' Cc='null' Bcc='null' ReplyTo='null' InReplyTo='null' MimeType='text-html' Encoding='UTF"));
    }
    
    @Test
    public void testSOAPProcessorThread() {
        Assert.assertEquals("SOAPProcessorThread#", 
                normalizer.getNormalizedThreadName("SOAPProcessorThread1ecb5c3ba2fcae4a4e3d1"));
    }
    
    @Test
    public void testCookieBrokerUpdates() {
        Assert.assertEquals("CookieBrokerUpdates#", 
                normalizer.getNormalizedThreadName("CookieBrokerUpdates-iwd4owfml6eth"));
    }
    
    @Test
    public void testC3P0PooledConnectionPoolManager() {
        Assert.assertEquals("C3P0PooledConnectionPoolManager#", 
                normalizer.getNormalizedThreadName("C3P0PooledConnectionPoolManager[identityToken->#bqq#hf#k#mickqhtf#b#r|#]-HelperThread-##"));
    }
    
    @Test
    public void testwildflyxnio() {
        Assert.assertEquals("xnio-file-watcher[#]-#", 
                normalizer.getNormalizedThreadName("xnio-file-watcher[Watcher for -usr-local-wildfly-standalone-tmp-vfs-temp-tempe#f#ad#-content-#-]-7364"));
    }
    
    @Test
    public void elasticsearch() {
        Assert.assertEquals("elasticsearch#", 
                normalizer.getNormalizedThreadName("elasticsearch[Speedball][transport_client_boss][T##]{New I-O boss ##}"));
    }
    
    @Test
    public void testOkHttp() {
        Assert.assertEquals("OkHttp https:#", 
                normalizer.getNormalizedThreadName("OkHttp https:--maps.googleapis.com-maps-api-geocode-json?client=gme-mycompany&latlng=#.#%#.#&signature=pkQTvo#Rqwz#yO#ceMvjT#qTY="));
    }
    
    @Test
    public void oldIO() {
        Assert.assertEquals("Old I-O client worker (#)", 
                normalizer.getNormalizedThreadName("Old I-O client worker ([id: #x#f#aa#f, -#.#.#.#:# => #-srv#.mycompany.net-#.#.#.#:#])"));
    }
    
    @Test
    public void akka() {
        Assert.assertEquals("default-akka.actor.default#", 
                normalizer.getNormalizedThreadName("default-akka.actor.default-dispatcher-# (Started: #, URL: https:--www.mycompany.com-abc-123-234-dfgd-asdf-dfasd, Phrase: asdfasd-234adfs-asdfas)"));
    }
    
    @Test
    public void testHashSessionManager() {
        Assert.assertEquals("org.eclipse.jetty.server.session.HashSessionManager#", 
                normalizer.getNormalizedThreadName("org.eclipse.jetty.server.session.HashSessionManager@2d3cfTimer"));
    }
    
    @Test
    public void testJobHandler() {
        Assert.assertEquals("JobHandler#", 
                normalizer.getNormalizedThreadName("JobHandler: -etc-workflow-instances-server#-#-#-#_#-request_for_activation_#:-content-mycompany-fr-ca-shared-header-header-slow-trucking"));
    }
    
    @Test
    public void testTransientResourceLock() {
        Assert.assertEquals("TransientResourceLock#", 
                normalizer.getNormalizedThreadName("TransientResourceLock-KAUQ#DGps#AAAFYtAgf#LJ"));
    }
    
    @Test
    public void jmb() {
        Assert.assertEquals("jbm-client-session#", 
                normalizer.getNormalizedThreadName("jbm-client-session-l3a-2wv3dwi-7-wtuxwvvi-nwp4im-qq5o2c3"));
    }

    @Test
    public void uri() {
        Assert.assertEquals("http-thingie-#-exec-# uri:#", 
                normalizer.getNormalizedThreadName("http-thingie-#-exec-# uri:-secure-QuickEditIssue!default.jspa username:A#AC#ZZ"));
    }
    
    @Test
    public void capsHttp() {
        Assert.assertEquals("http-thingie-#-exec-# HTTP:#", 
                normalizer.getNormalizedThreadName("http-thingie-#-exec-# HTTP:-secure-QuickEditIssue!default.jspa username:A#AC#ZZ"));
    }
    
    @Test
    public void brackets() {
        Assert.assertEquals("rt-#-ResultCollector-[#]",
                normalizer.getNormalizedThreadName("rt-#-ResultCollector-[[Hello World by GSS[#]] - MMMOAC#RF]"));
    }
    
    @Test
    public void multipleBrackets() {
        Assert.assertEquals("rt-#-ResultCollector-[#] value=[#] email=[#]",
                normalizer.getNormalizedThreadName("rt-#-ResultCollector-[test] value=[dude] email=[p@thing.com]"));
    }
}
