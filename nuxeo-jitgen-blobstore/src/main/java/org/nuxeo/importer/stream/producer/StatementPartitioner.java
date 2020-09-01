package org.nuxeo.importer.stream.producer;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.importer.stream.jit.USStateHelper;

public class StatementPartitioner {

	// raw statistics extracted from pre-generated IDcards
	protected static Map<String, Integer> initStateCount() {
		Map<String, Integer> sc = new HashMap<String, Integer>();
		sc.put("AL", 1805165);
		sc.put("AK", 1089932);
		sc.put("AZ", 1459695);
		sc.put("AR", 1661131);
		sc.put("CA", 4728759);
		sc.put("CO", 1397947);
		sc.put("CT", 354339);
		sc.put("DE", 230451);
		sc.put("DC", 5484);
		sc.put("FL", 2900218);
		sc.put("GA", 1943095);
		sc.put("HI", 489194);
		sc.put("ID", 678634);
		sc.put("IL", 4228646);
		sc.put("IN", 2129396);
		sc.put("IA", 3119288);
		sc.put("KS", 2059212);
		sc.put("KY", 1638839);
		sc.put("LA", 1452844);
		sc.put("ME", 126527);
		sc.put("MD", 1561654);
		sc.put("MA", 455589);
		sc.put("MI", 2119080);
		sc.put("MN", 2790243);
		sc.put("MS", 1119984);
		sc.put("MO", 3189422);
		sc.put("MT", 1132855);
		sc.put("NE", 1808829);
		sc.put("NV", 411183);
		sc.put("NH", 120673);
		sc.put("NJ", 1616523);
		sc.put("NM", 1381047);
		sc.put("NY", 3356576);
		sc.put("NC", 2277650);
		sc.put("ND", 1225929);
		sc.put("OH", 3762008);
		sc.put("OK", 2319335);
		sc.put("OR", 1204432);
		sc.put("PA", 5415338);
		sc.put("PR", 728411);
		sc.put("RI", 107525);
		sc.put("SC", 1222376);
		sc.put("SD", 1250660);
		sc.put("TN", 1310974);
		sc.put("TX", 5370142);
		sc.put("UT", 1018617);
		sc.put("VT", 208433);
		sc.put("VA", 1792387);
		sc.put("WA", 1943452);
		sc.put("WV", 1243889);
		sc.put("WI", 2412059);
		sc.put("WY", 621756);
		return sc;
	}

	public static class Partition {

		public List<String> states = new ArrayList<>();
		public int count = 0;
	}

	protected static StatementPartitioner instance;

	protected final List<Partition> ePartitions;
	protected final List<Partition> wPartitions;

	protected final Map<String, Integer> rawPartitions;

	protected StatementPartitioner() {

		Map<String, Integer> sourceData = initStateCount();

		Map<Integer, String> ec2s = new HashMap<>();
		Map<Integer, String> wc2s = new HashMap<>();

		for (String state : sourceData.keySet()) {
			int c = sourceData.get(state);
			if (USStateHelper.isEastern(state)) {
				//assertFalse(ec2s.containsKey(c));
				ec2s.put(c, state);
			} else {
				//assertFalse(wc2s.containsKey(c));
				wc2s.put(c, state);
			}
		}

		ePartitions = buildPartitions(ec2s);
		wPartitions = buildPartitions(wc2s);

		rawPartitions = new HashMap<>();
		int idx = 0;
		for (Partition p : ePartitions) {
			for (String state : p.states) {
				rawPartitions.put(state, idx);
			}
			idx++;
		}
		idx = 0;
		for (Partition p : wPartitions) {
			for (String state : p.states) {
				rawPartitions.put(state, idx);
			}
			idx++;
		}

	}

	public List<Partition> getEastPartitions() {
		return ePartitions;
	}

	public List<Partition> getWestPartitions() {
		return wPartitions;
	}

	protected List<Partition> buildPartitions(Map<Integer, String> c2s) {

		List<Partition> partitions = new ArrayList<>();

		List<Integer> sortedCount = new ArrayList<>(c2s.keySet());

		Collections.sort(sortedCount);
		Collections.reverse(sortedCount);

		Integer max = sortedCount.get(0);

		Partition p1 = new Partition();
		p1.states.add(c2s.get(max));
		p1.count = max;
		partitions.add(p1);

		max = new Long(Math.round(max * 1.1)).intValue();
		int i = 1;
		int j = sortedCount.size() - 1;

		while (i < j) {

			Partition p = new Partition();
			p.states.add(c2s.get(sortedCount.get(i)));
			p.count = sortedCount.get(i);
			partitions.add(p);

			while (p.count < max && i < j) {
				p.states.add(c2s.get(sortedCount.get(j)));
				p.count += sortedCount.get(j);
				j = j - 1;
			}
			i = i + 1;
		}
		if (i == j) {
			String remainingState = c2s.get(sortedCount.get(j));
			int remainingStateCount = sortedCount.get(j);
			// look for the lowest value
			int minValue = Integer.MAX_VALUE;
			int minIdx = -1;
			for (i = 0; i < partitions.size(); i++) {
				if (partitions.get(i).count < minValue) {
					minValue = partitions.get(i).count;
					minIdx = i;
				}
			}
			partitions.get(minIdx).count += remainingStateCount;
			partitions.get(minIdx).states.add(remainingState);
		}

		return partitions;

	}

	public static StatementPartitioner getInstance() {
		if (instance == null) {
			synchronized (StatementPartitioner.class) {
				if (instance == null) {
					instance = new StatementPartitioner();
				}
			}
		}
		return instance;
	}

	public int getPartition(String stateCode, String customerNumber, int partitionSize) {

		int rawPartitionsNumber = wPartitions.size();
		if (USStateHelper.isEastern(stateCode)) {
			rawPartitionsNumber = ePartitions.size();
		}

		int vpartitions = lcm(rawPartitionsNumber, partitionSize);
		int subpartitions = vpartitions / rawPartitionsNumber;

		int mainPartition = rawPartitions.get(stateCode);
		int sequence = getSequenceFromCustomerNumnber(customerNumber);

		return (mainPartition * subpartitions + sequence % subpartitions) % partitionSize;
	}

	protected int getSequenceFromCustomerNumnber(String customerNumber) {
		String suffix = customerNumber.substring(customerNumber.length() - 3);
		return Integer.parseInt(suffix, 16);
	}

	public static int lcm(int n1, int n2) {
		if (n1 == 0 || n2 == 0) {
			return 0;
		}
		int max = Math.max(n1, n2);
		int min = Math.min(n1, n2);
		int lcm = max;
		while (lcm % min != 0) {
			lcm += max;
		}
		return lcm;
	}
}
