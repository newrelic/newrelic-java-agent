package com.newrelic.instrumentation.labs.redisson;

public class Utils {
	
	static ThreadLocal<String> operation = new ThreadLocal<String>();
	static ThreadLocal<String> redissonType = new ThreadLocal<String>();
	static ThreadLocal<String> objectName = new ThreadLocal<String>();
	

	public static boolean operationIsSet() {
		String operationName = operation.get();
		return operationName != null && !operationName.isEmpty();
	}
	
	public static String getOperation() {
		String operationName = operation.get();
		return operationName;
	}
	
	public static void setOperation(String operationName) {
		operation.set(operationName);
	}
	
	public static void unSetOperation() {
		operation.remove();
	}
	
	public static boolean typeSet() {
		String type = redissonType.get();
		return type != null && !type.isEmpty();
	}
	
	public static void setType(Object obj) {
		Class<?> clazz = obj.getClass();
		String classname = clazz.getSimpleName().replace("Redisson", "").replace("Reactive", "");
		
		redissonType.set(classname);
	}
	
	public static void setType(String type) {
		redissonType.set(type);
	}
	
	public static String getType() {
		return redissonType.get();
	}
	
	public static void unSetType() {
		redissonType.remove();
	}
	
	public static boolean objectNameSet() {
		String oName = objectName.get();
		return oName != null && !oName.isEmpty();
	}
	
	public static void setObjectName(String oName) {
		if(oName.startsWith("{")) return;
		objectName.set(oName);
	}
	
	public static String getObjectName() {
		return objectName.get();
	}
	
	public static void unSetObjectName() {
		objectName.remove();
	}

}
