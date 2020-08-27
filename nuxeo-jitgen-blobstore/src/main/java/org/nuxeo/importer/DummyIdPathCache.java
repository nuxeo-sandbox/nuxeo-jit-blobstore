package org.nuxeo.importer;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;

public class DummyIdPathCache extends BasePath2IdCache {
	private static final long serialVersionUID = 1L;
	
	public DummyIdPathCache(int max) {
		super(max);
	}

	protected IdRef resolve(CoreSession session, String path) {

		if (path.endsWith("/01") || path.endsWith("/02") || path.endsWith("/03")) {
			resolveChildren(session, path.substring(0, path.length()-3), path.substring(path.length()-2));		
		}
		else {
			put(path, mkref(path));
		}
		return get(path);

	}
	protected void resolveChildren(CoreSession session, String parent, String key) {
		
		for (int i = 1; i < 4; i++) {
			String k = parent + "/0" + i;
			put(k, mkref(k));
		}
		
	}
	
	protected IdRef mkref(String path) {
		return new IdRef(UUID.randomUUID().toString());
	}

}
