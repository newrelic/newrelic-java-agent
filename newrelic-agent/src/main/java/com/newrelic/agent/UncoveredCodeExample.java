package com.newrelic.agent;


public class UncoveredCodeExample {

    private String field1;
    private int field2;
    private double field3;
    private boolean field4;
    private char field5;
    private long field6;
    private float field7;
    private short field8;
    private byte field9;
    private Object field10;

    public void missedMethod1() {
        field1 = "Uncovered example";
    }

    public int missedMethod2(int a, int b) {
        field2 = a + b;
        return field2;
    }

    public double missedMethod3(double a, double b) {
        field3 = a * b;
        return field3;
    }

    public void missedMethod4() {
        field4 = !field4;
    }

    public char missedMethod5(char input) {
        field5 = Character.toUpperCase(input);
        return field5;
    }

    public long missedMethod6(long a, long b) {
        field6 = a - b;
        return field6;
    }

    public float missedMethod7(float a, float b) {
        field7 = a / b;
        return field7;
    }

    public short missedMethod8(short a, short b) {
        field8 = (short) (a * b);
        return field8;
    }
}
