package org.nuxeo.data.gen.out.filter;

public abstract class AbstractFilter implements WriteFilter {

	protected int dpi = 150;

	public int getDPI() {
		return dpi;
	}

	public void setDPI(int dpi) {
		this.dpi = dpi;
	}

}
