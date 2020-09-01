package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.nuxeo.data.gen.meta.SequenceGenerator;
import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.importer.stream.producer.StatementPartitioner;
import org.nuxeo.importer.stream.producer.StatementPartitioner.Partition;

public class StateSplitter {
	
	@Test
	public void findRepartition() {

		StatementPartitioner partitioner = StatementPartitioner.getInstance();
		
		assertNotNull(partitioner);
		
		List<String> allStates = new ArrayList<>();
		
		int min = Integer.MAX_VALUE;
		int max = 0;
		for (Partition p : partitioner.getEastPartitions()) {
			for (String state: p.states) {
				assertFalse(allStates.contains(state));
				allStates.add(state);
			}
			if (p.count> max) {
				max = p.count;
			}
			if (p.count< min) {
				min = p.count;
			}
			System.out.println(p.states + " => " + p.count);
		}
		
		double delta = 100* (max-min)/max;
		System.out.println(delta);
		
		System.out.println("-----------------------------------------");
		
		min = Integer.MAX_VALUE;
		max = 0;				
		for (Partition p : partitioner.getWestPartitions()) {
			for (String state: p.states) {
				assertFalse(allStates.contains(state));
				allStates.add(state);
			}
			if (p.count> max) {
				max = p.count;
			}
			if (p.count< min) {
				min = p.count;
			}
			System.out.println(p.states + " => " + p.count);
		}
		delta = 100* (max-min)/max;
		System.out.println(delta);
		
		
		for (String state : USStateHelper.STATES) {
			assertTrue("State " + state + " is missing", allStates.contains(USStateHelper.getStateCode(state)));
		}
		
	}
	

	@Test
	public void testSequence() {
		
		SequenceGenerator sg = new SequenceGenerator(6);

		StatementPartitioner partitioner = StatementPartitioner.getInstance();
		
		Map<Integer, AtomicInteger> east = new HashMap<Integer, AtomicInteger>();
		Map<Integer, AtomicInteger> west = new HashMap<Integer, AtomicInteger>();
		
		int nbPartitions = 24;
		int nbStatements = 2000000;
		
		for (int i = 0; i < nbStatements; i++) {
			
			SequenceGenerator.Entry e =sg.next();
		
			
			String stateCode = USStateHelper.getStateCode(e.getMetaData()[3].trim());
			String customerNumber = e.getMetaData()[5].substring(0,19);
			
			int p = partitioner.getPartition(stateCode, customerNumber, nbPartitions);
			
			assertTrue(p < nbPartitions);
		
			Map<Integer, AtomicInteger> map = west;
			if (USStateHelper.isEastern(stateCode)) {
				map=east;
			}
			
			if (map.containsKey(p)) {
				map.get(p).incrementAndGet();
			} else {
				map.put(p, new AtomicInteger(1));
			}
		}
		
		dump(east);
		dump(west);
		
		int minE = Integer.MAX_VALUE;
		int maxE = 0;
		int minW = Integer.MAX_VALUE;
		int maxW = 0;

		int sum=0;
		for (int i = 0; i < nbPartitions; i++) {
			
			int v = east.get(i).intValue();
			maxE = Math.max(maxE, v);
			minE = Math.min(minE, v);
			sum+=v;
			
			v = west.get(i).intValue();
			maxW = Math.max(maxW, v);
			minW = Math.min(minW, v);
			sum+=v;
		}
		
		assertEquals(nbStatements, sum);
		double deltaE = 100* (maxE-minE)/maxE;
		System.out.println("Unbalance East = " + deltaE + "%");
		double deltaW = 100* (maxW-minW)/maxW;
		System.out.println("Unbalance West = " + deltaW + "%");
		
	}

	protected void dump(Map<Integer, AtomicInteger> map) {
		for (Integer i : map.keySet()) {
			System.out.println(i + " :" + map.get(i).intValue());
		}
	}
	
}


 	