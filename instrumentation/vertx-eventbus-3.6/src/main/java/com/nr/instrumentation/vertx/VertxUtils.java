package com.nr.instrumentation.vertx;

public class VertxUtils {

	private static final String REPLY = "vertx.reply";
	
	public static String normalize(String address) {
		if(address.contains(REPLY)) {
			return REPLY;
		} else if(tempAddress(address)) {
			return "TempAddress";
		}
		return address;
	}
	
	public static boolean replyAddress(String address) {
		return address.contains(REPLY);
	}
	
	public static boolean tempAddress(String address) {
		for(int i=0;i<address.length();i++) {
			char c = address.charAt(i);
			if(!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}

	
}
