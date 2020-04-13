package org.nuxeo.data.gen.meta;

import java.util.Date;
import java.util.GregorianCalendar;

public class FormatUtils {

	public static final int START_YEAR = 2020;

	public static String pad(String v, int size, boolean left) {
		if (v.length() > size) {
			v = v.substring(0, size - 1);
		}
		if (left) {
			return v + " ".repeat(size - v.length());
		} else {
			return " ".repeat(size - v.length()) + v;
		}
	}

	
	public static Date getDateWithOffset(int dm) {
		int dy = dm / 12;
		int m = dm - dy * 12;
		return new GregorianCalendar(START_YEAR - dy, m, 01).getTime();
	}

}
