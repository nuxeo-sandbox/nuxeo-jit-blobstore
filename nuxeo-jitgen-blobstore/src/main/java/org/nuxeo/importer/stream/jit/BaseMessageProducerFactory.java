package org.nuxeo.importer.stream.jit;

public class BaseMessageProducerFactory {

	public static final String SNOWBALL = "snowball";
	
	protected String getBlobStoreName(String repositoryName) {
		
		if (USStateHelper.EAST.equalsIgnoreCase(repositoryName)) {
			return SNOWBALL;
		}
		else if (USStateHelper.WEST.equalsIgnoreCase(repositoryName)) {
			return SNOWBALL;	
		}
		
		return repositoryName;		
	}
	
}
