package org.nuxeo.data.gen.out.filter;

import java.io.InputStream;

public interface WriteFilter {

	int getDPI();
	
	void setDPI(int dpi);
	
	String getName();
	
	InputStream wrap(byte[] pdf) throws Exception;
	
	String getExtension();
	
}
