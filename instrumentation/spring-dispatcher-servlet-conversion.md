# SpringDispatcherPointCut to Weave Module Conversion

## Overview

This document describes the changes needed to convert the legacy `SpringDispatcherPointCut` to a proper New Relic weave module.

## Original Pointcut Location

`newrelic-agent/src/main/java/com/newrelic/agent/instrumentation/pointcuts/frameworks/spring/SpringDispatcherPointCut.java`

## What the Pointcut Does

The `SpringDispatcherPointCut` instruments `org.springframework.web.servlet.DispatcherServlet` and captures:

### Target Methods

| Method | Signature (javax) | Signature (jakarta) |
|--------|-------------------|---------------------|
| `render` | `(Lorg/springframework/web/servlet/ModelAndView;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V` | `(Lorg/springframework/web/servlet/ModelAndView;Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V` |
| `doDispatch` | `(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V` | `(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V` |

### Behavior

1. **For `render` method:**
   - Extracts the view name from `ModelAndView` using `getViewName()`
   - Cleans the view name (handles redirects, forwards, HTTP URLs)
   - Creates metric: `SpringView/{viewName}` or fallback `SpringView/Java/{className}/render`

2. **For `doDispatch` method:**
   - Creates standard class/method metric using `ClassMethodMetricNameFormat`

## Conversion Strategy

### Target Modules

Add instrumentation to existing Spring modules:
- **spring-4.0.0**: For Spring 4.x-5.x with `javax.servlet` namespace
- **spring-6.0.0**: For Spring 6.x+ with `jakarta.servlet` namespace

### Files to Create

#### For spring-4.0.0 module:

1. **`src/main/java/com/nr/agent/instrumentation/SpringDispatcherUtil.java`**
```java
package com.nr.agent.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpringDispatcherUtil {
    private static final String REDIRECT_VIEW_SYNTAX = "/redirect:";
    private static final String FORWARD_VIEW_SYNTAX = "/forward:";
    private static final Pattern HTTP_PATTERN = Pattern.compile("(.*)https?://.*");

    public static String cleanViewName(String viewName) {
        if (viewName == null || viewName.isEmpty()) {
            return viewName;
        }
        if (viewName.charAt(0) != '/') {
            viewName = '/' + viewName;
        }
        if (viewName.startsWith(REDIRECT_VIEW_SYNTAX)) {
            return "/redirect:*";
        }
        if (viewName.startsWith(FORWARD_VIEW_SYNTAX)) {
            return null; // Let forwards be named after their destination view
        }
        int paramIndex = viewName.indexOf('?');
        if (paramIndex > 0) {
            viewName = viewName.substring(0, paramIndex);
        }
        Matcher paramDelimiterMatcher = HTTP_PATTERN.matcher(viewName);
        if (paramDelimiterMatcher.matches()) {
            viewName = paramDelimiterMatcher.group(1) + '*';
        }
        return viewName;
    }

    public static String getViewName(Object modelAndView) {
        if (modelAndView == null) {
            return null;
        }
        try {
            String viewName = (String) modelAndView.getClass().getMethod("getViewName").invoke(modelAndView);
            return cleanViewName(viewName);
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, "Unable to get Spring ModelAndView name", e);
            return null;
        }
    }
}
```

2. **`src/main/java/org/springframework/web/servlet/DispatcherServlet_Instrumentation.java`**
```java
package org.springframework.web.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.SpringDispatcherUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Weave(type = MatchType.ExactClass, originalName = "org.springframework.web.servlet.DispatcherServlet")
public abstract class DispatcherServlet_Instrumentation {

    @Trace
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Weaver.callOriginal();
    }

    @Trace
    protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String viewName = null;
        if (mv != null) {
            viewName = SpringDispatcherUtil.getViewName(mv);
        }

        if (viewName != null) {
            AgentBridge.getAgent().getTracedMethod().setMetricName("SpringView", viewName);
        } else {
            AgentBridge.getAgent().getTracedMethod().setMetricName("SpringView", "Java",
                    getClass().getName(), "render");
        }

        Weaver.callOriginal();
    }
}
```

#### For spring-6.0.0 module:

Same files but with `jakarta.servlet` imports instead of `javax.servlet`.

## Differences from Original Pointcut

| Aspect | Original Pointcut | Weave Module |
|--------|-------------------|--------------|
| Tracer creation | Custom `DefaultTracer` instantiation | `@Trace` annotation (standard tracer) |
| Metric name for doDispatch | `ClassMethodMetricNameFormat` (custom) | Default from `@Trace` |
| Metric name for render | `SimpleMetricNameFormat` | `setMetricName()` API |
| View name extraction | Uses `ModelAndView` interface or reflection | Reflection only (safer for weave) |

## Notes

- The weave module uses `@Trace` which creates standard tracers with transaction trace segments and scoped metrics
- The original pointcut's `canSetTransactionName()` and `setTransactionName()` methods were defined but unused in `doGetTracer` for the render case - these can be omitted
- The view name cleaning logic preserves the original behavior for redirects, forwards, and HTTP URLs

## After Conversion

Once the weave module is in place and verified working:
1. Remove `SpringDispatcherPointCut.java` from the agent
2. Update any references to `SpringPointCut.getModelAndViewViewName()` if needed
3. Test with various Spring versions (4.x, 5.x, 6.x)
