package org.nuxeo.data.gen.out;

import org.nuxeo.data.gen.out.filter.WriteFilter;

public interface BlobWriter {

	void write(byte[] data, String digest) throws Exception;
	
	void flush();
	
	public void setFilter(WriteFilter filter);
}
