/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Tiry
 */
package org.nuxeo.ecm.core.blob.jit.gen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.blob.BlobInfo;

public interface InMemoryBlobGenerator {

	String computeKey(Long accountSeed, Long dataSeed, Integer timeSeed);

	Map<String, String> getMetaDataKey(String key);

	InputStream getStream(String key) throws IOException;

	boolean readBlob(String key, Path dest) throws IOException;
	
	BlobInfo computeBlobInfo(String prefix, String key);
	
	DocInfo computeDocInfo(String prefix, Long accountSeed, Long dataSeed, Integer timeSeed);

	List<NodeInfo> getTimeHierarchy(int months,boolean childrenOnly);
}