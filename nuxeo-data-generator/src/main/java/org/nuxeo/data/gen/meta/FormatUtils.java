/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Tiry
 */

package org.nuxeo.data.gen.meta;

import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.lang3.StringUtils;

public class FormatUtils {

	public static final int START_YEAR = 2020;

	public static String pad(String v, int size, boolean left) {
		if (v.length() > size) {
			v = v.substring(0, size - 1);
		}
		if (left) {
			return v + StringUtils.repeat(" ", size - v.length());
		} else {
			return StringUtils.repeat(" ", size - v.length()) + v;
		}
	}

	public static Date getDateWithOffset(int dm) {
		int dy = dm / 12;
		int m = dm - dy * 12;
		return new GregorianCalendar(START_YEAR - dy, m, 01).getTime();
	}

}
