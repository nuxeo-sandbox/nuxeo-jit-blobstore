package org.nuxeo.importer;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;

public abstract class BasePath2IdCache extends LinkedHashMap<String, IdRef> {
	private static final long serialVersionUID = 1L;

	private final int max;

	protected long calls = 0;
	protected long hits = 0;

	public BasePath2IdCache(int max) {
		super(max, 1.0f, true);
		this.max = max;
	}

	@Override
	protected boolean removeEldestEntry(Entry<String, IdRef> eldest) {
		return size() > max;
	}

	public IdRef resolveWithCache(CoreSession session, String path) {

		calls++;
		
		IdRef ref = null;		
		synchronized (this) {
			ref=get(path);
		}		
		if (ref == null) {
			ref = resolve(session, path);
		} else {
			hits++;
		}
		return ref;
	}

	protected abstract IdRef resolve(CoreSession session, String path);

	public int getHitRatio() {
		return (int) ((100 * hits) / calls);
	}
}
