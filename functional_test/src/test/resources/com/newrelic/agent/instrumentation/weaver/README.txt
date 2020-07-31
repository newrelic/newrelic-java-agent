WeaveIntoAllMethods.class is compiled with java 8 -parameters.


package com.newrelic.agent.instrumentation.weaver;

public class WeaveIntoAllMethods {
    public static int callCount;

    public WeaveIntoAllMethods() {
    }

    public String oneParameter(String var1) {
        return null;
    }

    public void noParameters() {
    }

    public boolean threeParameters(int var1, boolean var2, char var3) {
        return false;
    }

    public void callAllMethods() {
        this.oneParameter("String");
        this.noParameters();
        this.threeParameters(3, false, 'c');
    }
}