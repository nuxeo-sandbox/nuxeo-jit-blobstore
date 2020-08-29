package org.nuxeo.data.gen.cli;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseHelper {

	private static final String ENTITY_TYPE = "entity-type";
	protected static final ObjectMapper MAPPER = new ObjectMapper();

	public static String formatAsString(String message) {
		return (String) format(message, true);
	}

	public static Map<String, Object> formatAsMap(String message) {
		return (Map<String, Object>) format(message, false);
	}

	protected static Object format(String message, boolean asString) {

		JsonNode node = null;
		try {
			node = MAPPER.readTree(message);
		} catch (Exception e) {
			e.printStackTrace();
			if (asString) {
				return message;
			} else {
				Map<String, Object> res = new HashMap<>();
				res.put("text", message);
				return res;
			}
		}
		if (node.has(ENTITY_TYPE)) {
			String type = node.get(ENTITY_TYPE).textValue();
			if ("string".equals(type)) {
				String text = node.get("value").textValue();
				if (text.startsWith("{")) {
					JsonNode subnode;
					try {
						subnode = MAPPER.readTree(text);
					} catch (Exception e) {
						e.printStackTrace();
						if (asString) {
							return text;
						} else {
							Map<String, Object> res = new HashMap<>();
							res.put("text", text);
							return res;
						}
					}
					if (asString) {
						return dumpAsString(subnode);
					} else {
						return dumpAsMap(subnode);
					}
				} else {
					if (asString) {
						return text;
					} else {
						Map<String, Object> res = new HashMap<>();
						res.put("text", text);
						return res;
					}
				}
			}
		}
		if (asString) {
			return dumpAsString(node);
		} else {
			return dumpAsMap(node);
		}

	}

	protected static String dumpAsString(JsonNode node) {
		StringBuffer sb = new StringBuffer();
		node.fieldNames().forEachRemaining(name -> {
			sb.append(name);
			sb.append(":");
			sb.append(node.get(name).asText());
			sb.append("\n");
		});
		return sb.toString();
	}

	protected static Map<String, Object> dumpAsMap(JsonNode node) {
		return MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {
		});
	}
}
