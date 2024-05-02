package io.opentelemetry.context;

public class MetricNames {
    static final String LAMBDA_CLASS_IDENTIFIER = "$$Lambda$";

    static String getMetricName(Class<?> clazz, String methodName) {
        final String className = trimClassName(clazz.getName());
        return getMetricName(trimClassName(className), methodName);
    }

    static String trimClassName(String className) {
        final int lambdaIndex = className.indexOf(LAMBDA_CLASS_IDENTIFIER);
        if (lambdaIndex > 0) {
            return className.substring(0, lambdaIndex) + LAMBDA_CLASS_IDENTIFIER;
        }
        return className;
    }

    static String getMetricName(String className, String methodName) {
        return "Java/" + className + '.' + methodName;
    }
}
