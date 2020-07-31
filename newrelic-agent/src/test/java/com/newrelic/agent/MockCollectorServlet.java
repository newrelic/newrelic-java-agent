/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.io.ByteStreams;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class MockCollectorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final int ERROR_DATA_SIZE_LIMIT_IN_BYTES = 1000000; // 1MB

    private static boolean inPingCommand = false;

    public MockCollectorServlet() {
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    @SuppressWarnings("unchecked")
    @Override
    protected synchronized void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("doPost");
        System.out.println("license_key=" + request.getParameter("license_key"));
        System.out.println("method=" + request.getParameter("method"));

        response.setHeader("Name", "Mock Collector Servlet");
        response.setContentType("application/json");
        PrintWriter pWriter = response.getWriter();
        JSONObject json = new JSONObject();
        if ("preconnect".equals(request.getParameter("method"))) {
            JSONObject map = new JSONObject();
            map.put("redirect_host", "localhost");
            map.put("security_policies", new JSONObject());
        } else if ("get_redirect_host".equals(request.getParameter("method"))) {
            json.put("return_value", "localhost");
        } else if ("connect".equals(request.getParameter("method"))) {
            if ("xxxxxxxxxxxxxxxxxxxxxxxxxxx".equals(request.getParameter("license_key"))) {
                response.setStatus(401);
                JSONObject inner = new JSONObject();
                inner.put("message", "Invalid license key, please contact support@newrelic.com");
                json.put("exception", inner);
            } else {
                JSONObject inner = new JSONObject();
                inner.put("agent_run_id", "1234567890");
                inner.put("collect_errors", Boolean.TRUE);
                inner.put("collect_traces", Boolean.TRUE);
                inner.put("data_report_period", 60);
                ArrayList<JSONObject> rulesList = new ArrayList<>();
                JSONObject rule = new JSONObject();
                rule.put("each_segment", Boolean.FALSE);
                rule.put("eval_order", 1000);
                rule.put("terminate_chain", Boolean.TRUE);
                rule.put("ignore", Boolean.FALSE);
                rule.put("match_expression", "foo");
                rule.put("replace_all", Boolean.FALSE);
                rule.put("replacement", "bar");

                rulesList.add(rule);
                inner.put("url_rules", rulesList);
                json.put("return_value", inner);
            }
        } else if ("get_agent_commands".equals(request.getParameter("method"))) {
            if (inPingCommand) {
                inPingCommand = false;
                // necessary value for the ping command test:
                @SuppressWarnings("rawtypes")
                ArrayList<ArrayList> inner = new ArrayList<>();
                ArrayList<Object> commands = new ArrayList<>();
                inner.add(commands);
                commands.add(1000);
                JSONObject pingCommand = new JSONObject();
                pingCommand.put("name", "ping");
                pingCommand.put("arguments", null);
                commands.add(pingCommand);
                json.put("return_value", inner);
            } else {
                ArrayList<String> inner = new ArrayList<>();
                json.put("return_value", inner);
            }
        } else if ("profile_data".equals(request.getParameter("method"))) {
            List<Integer> inner = Arrays.asList(1, 2);
            json.put("return_value", inner);
        } else if ("queue_ping_command".equals(request.getParameter("method"))) {
            inPingCommand = true;
            json.put("return_value", 1000);
        } else if ("metric_data".equals(request.getParameter("method"))) {
            ArrayList<String> inner = new ArrayList<>();
            json.put("return_value", inner);
        } else if ("agent_command_results".equals(request.getParameter("method"))) {
            json.put("return_value", null);
        } else if ("error_data".equals(request.getParameter("method"))) {
            byte[] bytes = ByteStreams.toByteArray(request.getInputStream());
            if (bytes.length > ERROR_DATA_SIZE_LIMIT_IN_BYTES) {
                response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                pWriter.close();
                return;
            }

            try {
                JSONParser parser = new JSONParser();
                GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
                InputStreamReader reader = new InputStreamReader(gzipInputStream);
                JSONArray errorData = (JSONArray) parser.parse(reader);
                json.put("return_value", ((JSONArray) errorData.get(1)).size()); // Send back the number of error traces
            } catch (Exception e) {
                try {
                    // Try inflating first
                    InputStreamReader reader = new InputStreamReader(
                            new InflaterInputStream(new ByteArrayInputStream(bytes), new Inflater(true)), StandardCharsets.UTF_8);
                    JSONParser parser = new JSONParser();
                    JSONArray errorData = (JSONArray) parser.parse(reader);
                    json.put("return_value", ((JSONArray) errorData.get(1)).size()); // Send back the number of error traces
                } catch (Exception e2) {
                    json.put("return_value", 0);
                }
            }
        } else {
            json.put("return_value", "hello world");
        }
        pWriter.print(json);
        System.out.println(json);
        pWriter.close();
    }

}
