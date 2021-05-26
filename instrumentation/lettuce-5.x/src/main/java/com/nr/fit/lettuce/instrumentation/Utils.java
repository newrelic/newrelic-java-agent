package com.nr.fit.lettuce.instrumentation;

public class Utils {

	
	private static Utils instance = null;
	public static boolean initialized = false;

	public static void init() {
		if(instance == null) {
			instance = new Utils();
			initialized = true;
		}
	}
	
}
