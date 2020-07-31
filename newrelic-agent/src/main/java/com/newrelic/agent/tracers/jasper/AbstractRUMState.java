/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

import java.text.MessageFormat;
import java.util.regex.Pattern;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;

public abstract class AbstractRUMState implements RUMState {

    protected static final String BROWSER_TIMING_HEADER_CODE_SNIPPET = getMethodInvocationCode("getBrowserTimingHeaderForContentType");
    protected static final String BROWSER_TIMING_FOOTER_CODE_SNIPPET = getMethodInvocationCode("getBrowserTimingFooterForContentType");

    protected static final Pattern HEAD_PATTERN = Pattern.compile("<head(?!er)[^>]*>", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);
    protected static final Pattern HEAD_END_PATTERN = Pattern.compile("</head(?!er)[^>]*>", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);
    protected static final Pattern BODY_END_PATTERN = Pattern.compile("</body[^>]*>", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);
    protected static final Pattern BODY_START_PATTERN = Pattern.compile("<body[^>]*>", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);
    protected static final Pattern HTML_END_PATTERN = Pattern.compile("</html[^>]*>", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);
    protected static final Pattern NOT_META_PATTERN = Pattern.compile("<(?![mM][eE][tT][aA]\\s)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    protected static final Pattern SCRIPT_PATTERN = Pattern.compile("<script", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);
    protected static final Pattern END_SCRIPT_PATTERN = Pattern.compile("</script>", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);
    protected static final Pattern START_TAG_PATTERN = Pattern.compile("<");
    protected static final Pattern END_TAG_OR_QUOTE_PATTERN = Pattern.compile("(>|\"|')");
    protected static final Pattern QUOTE_PATTERN = Pattern.compile("\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    protected static final Pattern SINGLE_QUOTE_PATTERN = Pattern.compile("'", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);
    protected static final Pattern END_COMMENT = Pattern.compile("-->", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    protected static final Pattern TITLE_END = Pattern.compile("</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    protected static final RUMState HEAD_STATE = new HeadState();
    protected static final RUMState PRE_META_STATE = new PreMetaState();
    protected static final RUMState QUOTE_STATE = new QuoteSate();
    protected static final RUMState SINGLE_QUOTE_STATE = new SingleQuoteState();
    protected static final RUMState COMMENT_STATE = new CommentState();
    protected static final RUMState TITLE_STATE = new TitleState();
    protected static final RUMState META_STATE = new MetaState();
    protected static final RUMState BODY_STATE = new BodyState();
    protected static final RUMState DONE_STATE = new DoneState();
    protected static final RUMState SCRIPT_STATE = new ScriptPostHeaderState();
    protected static final RUMState PRE_HEAD_SCRIPT_STATE = new ScriptPreHeaderState();

    protected static String getMethodInvocationCode(String methodName) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("try {\n");
        sb.append("  java.lang.Class __nrClass = Class.forName(\"com.newrelic.api.agent.NewRelicApiImplementation\");\n");
        sb.append("  java.lang.reflect.Method __nrMethod = __nrClass.getMethod(\"").append(methodName).append(
                "\", new Class[]{ String.class });\n");
        sb.append("  out.write((String) __nrMethod.invoke(null, new Object[]{ null }));\n");
        sb.append("} catch (Throwable __t) {\n");
        sb.append("}\n");
        return sb.toString();
    }

    protected void writeText(Transaction tx, GenerateVisitor generator, TemplateText node, String text)
            throws Exception {
        node.setText(text);
        generator.visit(node);

        // the method we're overriding will want to print the original node. empty its text
        node.setText("");
    }

    protected void writeHeader(GenerateVisitor generator) throws Exception {
        generator.writeScriptlet(BROWSER_TIMING_HEADER_CODE_SNIPPET);
    }

    protected void writeFooter(GenerateVisitor generator) throws Exception {
        generator.writeScriptlet(BROWSER_TIMING_FOOTER_CODE_SNIPPET);
    }

    protected void writeHeader(Transaction tx, GenerateVisitor generator, TemplateText node, String text, int end)
            throws Exception {
        String jspFile = GeneratorVisitTracerFactory.getPage(tx);
        String msg = MessageFormat.format("Injecting browser timing header into: {0}", jspFile);
        Agent.LOG.fine(msg);

        String s = text.substring(0, end);
        node.setText(s);
        generator.visit(node);

        generator.writeScriptlet(BROWSER_TIMING_HEADER_CODE_SNIPPET);

        s = text.substring(end);
        node.setText(s);
        generator.visit(node);

        // the method we're overriding will want to print the original node. empty its text
        node.setText("");
    }

    protected void writeFooter(Transaction tx, GenerateVisitor generator, TemplateText node, String text, int end)
            throws Exception {

        String jspFile = GeneratorVisitTracerFactory.getPage(tx);
        String msg = MessageFormat.format("Injecting browser timing footer into: {0}", jspFile);
        Agent.LOG.fine(msg);

        String s = text.substring(0, end);
        node.setText(s);
        generator.visit(node);

        generator.writeScriptlet(BROWSER_TIMING_FOOTER_CODE_SNIPPET);

        s = text.substring(end);
        node.setText(s);
        generator.visit(node);

        // the method we're overriding will want to print the original node.
        // empty its text
        node.setText("");
    }
}
