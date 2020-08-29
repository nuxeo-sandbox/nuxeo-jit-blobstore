package org.nuxeo.importer.stream.jit.automation;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.lib.stream.pattern.producer.ProducerStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProducerStatusHelper {

	static Field failField;
	
	protected static final ObjectMapper MAPPER = new ObjectMapper();
	
	public static void init() throws OperationException {
		if (failField==null) {
			try {
				failField = ProducerStatus.class.getDeclaredField("fail");
			} catch (Exception e) {
				throw new OperationException("Unable to init ProducerStatus formatter", e);
			} 
			failField.setAccessible(true);
		}
	}
	
	public static Map<String,Serializable> aggregate(List<ProducerStatus> stats) {
		long startTime = stats.stream().mapToLong(r -> r.startTime).min().orElse(0);
		long stopTime = stats.stream().mapToLong(r -> r.stopTime).max().orElse(0);
		double elapsed = (stopTime - startTime) / 1000.;
		long messages = stats.stream().mapToLong(r -> r.nbProcessed).sum();
		double mps = (elapsed != 0) ? messages / elapsed : 0.0;
		int producers = stats.size();
		
		long failures = stats.stream().filter(s ->  {
				try {
					return (boolean) failField.get(s);
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}).count();
		
		Map<String,Serializable> result = new HashMap<>();
		
		result.put("producers", producers);
		result.put("failures", failures);
		result.put("messages", messages);
		result.put("elapsed", elapsed);
		result.put("throughput", mps);
		
		return result;
	}
	
	public static String aggregateJSON(List<ProducerStatus> stats) throws OperationException {
		
		Map<String,Serializable> map = aggregate(stats);
		try {
			return MAPPER.writeValueAsString(map);
		} catch (JsonProcessingException e) {
			throw new OperationException("Unable to serialize ProcuderResult", e);
		}
	}
		
	
}
