package org.nuxeo.data.gen.cli;

import java.util.Iterator;
import java.util.LinkedHashSet;

public class KeyBuffer extends LinkedHashSet<String> {

	private static final long serialVersionUID = 1L;
	
	private long max;
	
	protected int bucketsize=1000;

	public KeyBuffer(long max) {
        this(max,  1 + Math.toIntExact(max/10));
    }

	public KeyBuffer(long max, int bucketsize) {
        this.max = max;
        this.bucketsize=bucketsize;
    }

	@Override
	public boolean add(String item) {
		if (size() >= max) {
			removeOlds();
		}
		return super.add(item);
	}

	private void removeOlds() {
        if(size() > 0) {
            Iterator<String> iterator = iterator();
            String item=null;
            for (int i = 0 ; i < bucketsize; i++) {
            	if (!iterator.hasNext()) return;
            	iterator.next();
                iterator.remove();
            }
        }
    }
}
