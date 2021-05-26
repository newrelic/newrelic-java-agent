package com.nr.fit.lettuce.instrumentation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StringUtils {

	public static String getCSS(List<?> list) {
		StringBuffer sb = new StringBuffer();
		int i = 0;
		int size = list.size();
		for (Object obj : list) {
			sb.append(obj);
			if (i < size - 1) {
				sb.append(',');
			}
		}
		return sb.toString();
	}

	public static String getKeysAsCSS(Map<?, ?> map) {
		StringBuffer sb = new StringBuffer();
		int i = 0;
		Set<?> keys = map.keySet();
		int size = keys.size();
		for (Object obj : keys) {
			sb.append(obj);
			if (i < size - 1) {
				sb.append(',');
			}
		}
		return sb.toString();

	}

	public static String getCSSFromArray(Object[] objects) {
		if (objects == null || objects.length == 0)
			return " ";
		List<Object> objectList = Arrays.asList(objects);
		return getCSS(objectList);
	}
}
