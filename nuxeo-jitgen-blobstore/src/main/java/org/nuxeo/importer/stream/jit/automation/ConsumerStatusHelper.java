package org.nuxeo.importer.stream.jit.automation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConsumerStatusHelper {

	protected static final ObjectMapper MAPPER = new ObjectMapper();

	public static Map<String, Serializable> aggregate(List<ConsumerStatus> stats) {

		long startTime = stats.stream().mapToLong(r -> r.startTime).min().orElse(0);
		long stopTime = stats.stream().mapToLong(r -> r.stopTime).max().orElse(0);
		double elapsed = (stopTime - startTime) / 1000.;
		long committed = stats.stream().mapToLong(r -> r.committed).sum();
		double mps = (elapsed != 0) ? committed / elapsed : 0.0;
		int consumers = stats.size();
		long failures = stats.stream().filter(s -> s.fail).count();

		Map<String, Serializable> result = new HashMap<>();

		result.put("consumers", consumers);
		result.put("failures", failures);
		result.put("committed", committed);
		result.put("elapsed", elapsed);
		result.put("throughput", mps);

		return result;
	}

	public static String aggregateJSON(List<ConsumerStatus> stats, String batchId) throws OperationException {

		Map<String, Serializable> map = aggregate(stats);
		if (batchId!=null) {
			map.put("bulkIndexingCommandId", batchId);
		}
		try {
			return MAPPER.writeValueAsString(map);
		} catch (JsonProcessingException e) {
			throw new OperationException("Unable to serialize ProcuderResult", e);
		}
	}

}
