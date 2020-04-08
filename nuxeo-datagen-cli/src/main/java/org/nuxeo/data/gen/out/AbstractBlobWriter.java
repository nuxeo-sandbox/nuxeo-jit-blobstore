package org.nuxeo.data.gen.out;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.nuxeo.data.gen.out.filter.WriteFilter;

public abstract class AbstractBlobWriter implements BlobWriter {

	protected WriteFilter filter;
	
	public WriteFilter getFilter() {
		return filter;
	}

	public void setFilter(WriteFilter filter) {
		this.filter = filter;
	}

	protected InputStream wrap(byte[] pdf) throws Exception {
		if (filter==null) {
			return new ByteArrayInputStream(pdf);
		} else {
			return filter.wrap(pdf);
		}
	}
	
	protected String getExtension() {
		if (filter==null) {
			return "pdf";
		} else {
			return filter.getExtension();
		}
	}
	
}
