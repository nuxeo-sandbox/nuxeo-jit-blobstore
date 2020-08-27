package org.nuxeo.importer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;

public class StmtId2PathCache extends BasePath2IdCache {
	private static final long serialVersionUID = 1L;
	
	public StmtId2PathCache(int max) {
		super(max);
	}

	protected IdRef resolve(CoreSession session, String path) {

		if (path.endsWith("/01") || path.endsWith("/02") || path.endsWith("/03")) {
			return resolveChildrenAccounts(session, path.substring(0, path.length()-3), path.substring(path.length()-2));		
		}
		else {
			IdRef ref = (IdRef) session.getDocument(new PathRef(path)).getRef();
			synchronized (this) {
				put(path, ref);	
			}			
			return ref;
		}
	}
	
	protected IdRef __resolveChildrenAccounts(CoreSession session, String parent, String key) {
		
		DocumentModelList children = session.getChildren(new PathRef(parent), "Account");
	
		IdRef result = null;
		synchronized (this) {
			for (DocumentModel child:children) {
				put(child.getPathAsString(), (IdRef) child.getRef());
				if (child.getName().equals(key)) {
					result=(IdRef) child.getRef();
				}
			}			
		}
		return result;
	}

	protected IdRef resolveChildrenAccounts(CoreSession session, String parent, String key) {
	
		IdRef result = null;
	
	    try (IterableQueryResult accounts = session.queryAndFetch("SELECT ecm:uuid, ecm:name FROM Account where ecm:path STARTSWITH '" + parent + "'" , NXQL.NXQL);) {
	    	synchronized (this) {	   		
		    	for (Map<String, Serializable> map : accounts) {
	            	String p = new Path(parent).append((String) map.get("ecm:name")).toString();
	            	put(p, (IdRef) new IdRef((String)map.get("ecm:uuid")));
					if (map.get("ecm:name").equals(key)) {
						result=new IdRef((String)map.get("ecm:uuid"));
					}
	            }
	    	}
        }
		return result;
	}
	
}
