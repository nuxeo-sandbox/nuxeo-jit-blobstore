package org.nuxeo.ecm.core.blob.jit.rnd.key;

public interface KeyCodec {
	
	public String encodeSeeds(Long s1, Long s2, Integer m);

	public Long[] decodeSeeds(String key);

}
