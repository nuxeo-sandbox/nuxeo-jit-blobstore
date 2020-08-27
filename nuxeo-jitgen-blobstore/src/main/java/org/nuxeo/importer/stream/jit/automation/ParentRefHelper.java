package org.nuxeo.importer.stream.jit.automation;

import java.lang.reflect.Field;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.importer.StmtId2PathCache;

public class ParentRefHelper {

	protected static ParentRefHelper instance;	
	
	protected Field parentRefField;
	
	protected StmtId2PathCache path2Ids = new StmtId2PathCache(1000);
	
	public static void init() throws Exception {		
		Field parentRefField = DocumentModelImpl.class.getDeclaredField("parentRef");
		parentRefField.setAccessible(true);		
		instance = new ParentRefHelper(parentRefField);
	}
	
	public static ParentRefHelper getInstance() {
		return instance;
	}
	
	protected ParentRefHelper(Field parentRefField) {
		this.parentRefField=parentRefField;
	}
	
	public void setParentRef(CoreSession session, DocumentModel doc, String path) {
	
		IdRef idRef = resolveFromCache(session, path);
		
		try {
			parentRefField.set(doc, idRef);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
	}

	public IdRef getFromCache(String path) {
		return path2Ids.get(path);
	}
	public IdRef resolveFromCache(CoreSession session, String path) {
		return path2Ids.resolveWithCache(session, path);
	}
}
