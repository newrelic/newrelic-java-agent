# spring-4.2.0 Instrumentation Module

### HandlerInterceptor Instrumentation

This module contains instrumentation for the `org.springframework.web.servlet.HandlerInterceptor` interface that replaces
the legacy pointcut instrumentation. The weave class differs from the pointcut in that it now will instrument
the `default` methods on the interface; the pointcut method did not. This means that when an implementing class relies on
a default method (doesn't override it), that call is now traced. As a result, the instrumentation coverage
is more complete and there will be `preHandle`, `postHandle` and `afterCompletion` segments.

Another change is that the roll up metric of `Spring/HandlerInterceptor` was a scoped metric with the pointcut
instrumentation. It is now an unscoped metric.