package org.nuxeo.importer.schemamanager;

import java.util.Optional;
import java.util.Set;

import org.nuxeo.ecm.core.schema.PropertyCharacteristicHandler;
import org.nuxeo.runtime.model.DefaultComponent;

public class NoCheckPropertyCharacteristicHandler extends DefaultComponent implements PropertyCharacteristicHandler {

	@Override
	public boolean isSecured(String schema, String path) {
		// that was an easy check!
		return false;
	}

	@Override
	public Set<String> getDeprecatedProperties(String arg0) {
		return null;
	}

	@Override
	public Optional<String> getFallback(String arg0, String arg1) {
		return null;
	}

	@Override
	public Set<String> getRemovedProperties(String arg0) {
		return null;
	}

	@Override
	public boolean isDeprecated(String arg0, String arg1) {
		return false;
	}

	@Override
	public boolean isRemoved(String arg0, String arg1) {
		return false;
	}
	
}
