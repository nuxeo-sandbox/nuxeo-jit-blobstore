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

import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;

public class DocInfo extends NodeInfo {

	public BlobInfo blobInfo;

	public Map<String, String> metaData;

	public String key;

	public Blob getBlob() {
		if (blobInfo == null)
			return null;
		return new SimpleManagedBlob(blobInfo);
	}

	public String getMeta(String name) {
		if (metaData == null)
			return null;
		return metaData.get(name);
	}

	public String getKey() {
		return key;
	}
	
	public String getFileName() {
		if (blobInfo == null)
			return null;
		return blobInfo.filename;
	}
	
	public Set<String> getMetaDataKeys() {
		return metaData.keySet();
	}
}
