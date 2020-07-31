package org.nuxeo.importer.stream.jit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class USStateHelper {

	public static final String EAST = "us-east";
	public static final String WEST = "us-west";	
	
	public static String toPath(String stateName) {
		if ("fl".equals(stateName)) {
			return "florida";
		}
		return stateName.trim().toLowerCase().replace(" ", "-");		
	}

	public static int getOffset(String state) {
		state=state.trim().toLowerCase();		
		Integer idx = StateName2Idx.get(state);
		if (idx !=null) {
			return idx;
		}
		if ("fl".equals(state)) {
			return 9;
		}
		return -1;
	}
	
	// list of states as referenced in the CSV data file
	public static final String[] STATES = new String[] {
			"Alabama",
			"Alaska",
			"Arizona",
			"Arkansas",
			"California",
			"Colorado",
			"Connecticut",
			"Delaware",
			"District of Columbia",
			"Florida",
			"Georgia",
			"Hawaii",
			"Idaho",
			"Illinois",
			"Indiana",
			"Iowa",
			"Kansas",
			"Kentucky",
			"Louisiana",
			"Maine",
			"Maryland",
			"Massachusetts",
			"Michigan",
			"Minnesota",
			"Mississippi",
			"Missouri",
			"Montana",
			"Nebraska",
			"Nevada",
			"New Hampshire",
			"New Jersey",
			"New Mexico",
			"New York",
			"North Carolina",
			"North Dakota",
			"Ohio",
			"Oklahoma",
			"Oregon",
			"Pennsylvania",
			"Puerto Rico",
			"Rhode Island",
			"South Carolina",
			"South Dakota",
			"Tennessee",
			"Texas",
			"Utah",
			"Vermont",
			"Virginia",
			"Washington",
			"West Virginia",
			"Wisconsin",
			"Wyoming"
	};
	
	// list of states as to be put on the "Est side"
	public static final List<String> EAST_STATES_CODE =  new ArrayList<String>( Arrays.asList (
				"ME",
				"VT",
				"NH",
				"MA",
				"CT",
				"NJ",
				"NY",
				"RI",
				"PA",
				"VA",
				"NC",
				"SC",
				"GA",
				"FL",
				"AL",
				"MS",
				"TN",
				"KY",
				"WV",
				"OH",
				"IN",
				"MI",
				"IN",
				"VA",
				"MD"));
	
	protected static Map<String, String> StateName2StateCode = initStateCode();
	
	protected static Map<String, Integer> StateName2Idx = initStateIdx();

	protected static Map<String, Integer> initStateIdx() {
		Map<String, Integer> s2i = new HashMap<String, Integer>();
		for (int i =0; i < STATES.length; i++) {
			s2i.put(STATES[i].trim().toLowerCase(), i);
		}
		return s2i;
	}
	
	public static String getStateCode(String stateName) {
		stateName = stateName.trim();
		String code =  StateName2StateCode.get(stateName);		
		if (code!=null) {
			return code;
		}
		return stateName;
	}	
	
	public static boolean isEastern(String stateCode) {
		return EAST_STATES_CODE.contains(stateCode);
	}
	
	protected static Map<String, String> initStateCode() {
		Map<String, String> s2c = new HashMap<String, String>();
		s2c.put("Alabama","AL");
		s2c.put("Alaska","AK");
		s2c.put("Arizona","AZ");
		s2c.put("Arkansas","AR");
		s2c.put("California","CA");
		s2c.put("Colorado","CO");
		s2c.put("Connecticut","CT");
		s2c.put("Delaware","DE");
		s2c.put("Florida","FL");
		s2c.put("Georgia","GA");
		s2c.put("Hawaii","HI");
		s2c.put("Idaho","ID");
		s2c.put("Illinois","IL");
		s2c.put("Indiana","IN");
		s2c.put("Iowa","IA");
		s2c.put("Kansas","KS");
		s2c.put("Kentucky","KY");
		s2c.put("Louisiana","LA");
		s2c.put("Maine","ME");
		s2c.put("Maryland","MD");
		s2c.put("Massachusetts","MA");
		s2c.put("Michigan","MI");
		s2c.put("Minnesota","MN");
		s2c.put("Mississippi","MS");
		s2c.put("Missouri","MO");
		s2c.put("Montana","MT");
		s2c.put("Nebraska","NE");
		s2c.put("Nevada","NV");
		s2c.put("New Hampshire","NH");
		s2c.put("New Jersey","NJ");
		s2c.put("New Mexico","NM");
		s2c.put("New York","NY");
		s2c.put("North Carolina","NC");
		s2c.put("North Dakota","ND");
		s2c.put("Ohio","OH");
		s2c.put("Oklahoma","OK");
		s2c.put("Oregon","OR");
		s2c.put("Pennsylvania","PA");
		s2c.put("Rhode Island","RI");
		s2c.put("South Carolina","SC");
		s2c.put("South Dakota","SD");
		s2c.put("Tennessee","TN");
		s2c.put("Texas","TX");
		s2c.put("Utah","UT");
		s2c.put("Vermont","VT");
		s2c.put("Virginia","VA");
		s2c.put("Washington","WA");
		s2c.put("West Virginia","WV");
		s2c.put("Wisconsin","WI");
		s2c.put("Wyoming","WY");
		s2c.put("District of Columbia","DC");	
		s2c.put("Puerto Rico","PR");	
		
		Map<String, String> alias = new HashMap<>();
		
		for (String name : s2c.keySet()) {
			alias.put(name.toLowerCase(), s2c.get(name));
			alias.put(toPath(name), s2c.get(name));
		}
	
		s2c.putAll(alias);
		
		return s2c;
	}
	
}
