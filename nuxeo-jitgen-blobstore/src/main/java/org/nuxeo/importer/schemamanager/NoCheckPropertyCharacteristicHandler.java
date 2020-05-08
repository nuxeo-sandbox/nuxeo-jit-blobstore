package org.nuxeo.importer.schemamanager;

import org.nuxeo.ecm.core.schema.PropertyCharacteristicHandler;
import org.nuxeo.runtime.model.DefaultComponent;

public class NoCheckPropertyCharacteristicHandler extends DefaultComponent implements PropertyCharacteristicHandler {

	@Override
	public boolean isSecured(String schema, String path) {
		// that was an easy check!
		return true;
	}

}
