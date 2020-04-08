package org.nuxeo.data.gen.out;

public interface BlobWriter {

	void write(byte[] data, String digest) throws Exception;
	
	void flush();
}
