package org.nuxeo.ecm.core.blob.jit.gen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

public interface InMemoryBlobGenerator {

	String computeKey(Long accountSeed, Long dataSeed, Integer timeSeed);

	Map<String, String> getMetaDataKey(String key);

	InputStream getStream(String key) throws IOException;

	boolean readBlob(String key, Path dest) throws IOException;

}