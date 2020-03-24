package org.nuxeo.ecm.core.blob.jit.rnd.key;

public class DummyKeyCodec implements KeyCodec {

	@Override
	public String encodeSeeds(Long seed1, Long seed2, Integer dm) {
		StringBuffer sb = new StringBuffer();
		
		sb.append(String.format("%019d", seed1));
		if (seed2!=null) {
			sb.append(":");
			sb.append(String.format("%019d", seed2));
		}
		sb.append(":");
		sb.append(String.format("%04d", dm));
		return sb.toString();
	}

	@Override
	public Long[] decodeSeeds(String key) {
		String[] parts = key.split(":");		
		return new Long[]{Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2])};		
	}

}
