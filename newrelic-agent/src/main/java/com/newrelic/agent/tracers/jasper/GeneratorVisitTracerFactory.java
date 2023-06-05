/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

import java.text.MessageFormat;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;

public class GeneratorVisitTracerFactory extends AbstractTracerFactory {

    protected static final String RUM_STATE_PROCESSOR_KEY = RUMStateProcessor.class.getName();
    private static final String UNKNOWN_JSP = "UNKNOWN";
    private static final Pattern COMMENT_PATTERN = Pattern.compile("<!\\s*--.*?--\\s*\\>", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);

    public GeneratorVisitTracerFactory() {
        super();
    }

    static boolean isAutoInstrumentationEnabled() {
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        return config.getBrowserMonitoringConfig().isAutoInstrumentEnabled();
    }

    public static void noticeJspCompile(Transaction tx, String page) {
        tx.getInternalParameters().put(RUMStateProcessor.class.getName(), new RUMStateProcessor(page));
    }

    public static String getPage(Transaction tx) {
        RUMStateProcessor processor = (RUMStateProcessor) tx.getInternalParameters().get(RUM_STATE_PROCESSOR_KEY);
        return processor.getPage();
    }

    public static boolean isIgnorePageForAutoInstrument(String page, Transaction tx) {
        Set<String> pages = ServiceFactory.getConfigService().getDefaultAgentConfig().getBrowserMonitoringConfig().getDisabledAutoPages();
        if (page != null) {
            for (String current : pages) {
                if (current.equals(page)) {
                    logIgnoredPage(page, tx);
                    return true;
                }
            }
        }
        return false;
    }

    private static void logIgnoredPage(String page, Transaction tx) {
        if (tx != null) {
            if (tx.getInternalParameters().get(page) == null) {
                tx.getInternalParameters().put(page, Boolean.TRUE);
                Agent.LOG.fine(MessageFormat.format("Ignoring page {0} for auto RUM instrumentation", page));
            }
        }
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object generator, Object[] args) {
        if (!isAutoInstrumentationEnabled()) {
            return null;
        }
        Object node = args[0];
        try {
            // check if we should ignore the page
            String page = getPage(transaction);
            if (isIgnorePageForAutoInstrument(page, transaction)) {
                return null;
            }

            JasperClassFactory factory = JasperClassFactory.getJasperClassFactory(generator.getClass().getClassLoader());
            GenerateVisitor generateVisitor = factory.getGenerateVisitor(generator);
            TemplateText templateText = factory.getTemplateText(node);
            processText(transaction, generateVisitor, templateText);
        } catch (Exception e) {
            Agent.LOG.log(Level.FINE, "An error occurred auto enabling real user monitoring", e);
        }
        return null;
    }

    void processText(Transaction tx, GenerateVisitor generator, TemplateText node) throws Exception {
        String text = node.getText();
        if (text != null && text.length() > 0) {
            RUMStateProcessor state = (RUMStateProcessor) tx.getInternalParameters().get(RUM_STATE_PROCESSOR_KEY);
            if (state == null) {
                state = new RUMStateProcessor(UNKNOWN_JSP);
                tx.getInternalParameters().put(RUMStateProcessor.class.getName(), state);
            }
            state.process(tx, generator, node, text);
        }
    }

    private static class RUMStateProcessor extends AbstractRUMState {

        private final String page;
        private boolean inProgress = false;
        private RUMState currentState = HEAD_STATE;

        private RUMStateProcessor(String page) {
            this.page = page;
        }

        String getPage() {
            return page;
        }

        @Override
        public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text)
                throws Exception {
            if (inProgress) {
                return this;
            }
            try {
                inProgress = true;
                int start = 0;
                Matcher matcher = COMMENT_PATTERN.matcher(text);
                while (matcher.find()) {
                    String comment = text.substring(matcher.start(), matcher.end());
                    String notComment = text.substring(start, matcher.start());
                    start = matcher.end();
                    if (notComment.length() > 0) {
                        currentState = currentState.process(tx, generator, node, notComment);
                    }
                    writeText(tx, generator, node, comment);
                }
                String notComment = text.substring(start, text.length());
                if (notComment.length() > 0) {
                    currentState = currentState.process(tx, generator, node, notComment);
                }
            } finally {
                inProgress = false;
            }
            return this;
        }
    }

}