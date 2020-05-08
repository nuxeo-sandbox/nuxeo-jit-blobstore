package org.nuxeo.importer.version;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.versioning.StandardVersioningService;

public class ImportVersioningService extends StandardVersioningService {

	@Override
    protected void setInitialVersion(Document doc) {
		// simple rule really
        setVersion(doc, 1, 0);
    }
	
}
