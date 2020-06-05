package org.nuxeo.data.gen;

import org.apache.commons.lang3.StringUtils;

public class BaseBankTemplate {

	public static String mkTag(String value, int size) {
		String tag = "#" + value;
		return tag + StringUtils.repeat("-", size - 1 - tag.length()) + "#";
	}

	public static final String[] _keys = new String[] {
			mkTag("NAME", 41), 
			mkTag("STREET", 20), 
			mkTag("CITY", 20),
			mkTag("STATE", 20), 
			mkTag("DATE", 20), 
			mkTag("ACCOUNTID", 22), };

	public String[] getKeys() {
		return _keys;
	}

}
