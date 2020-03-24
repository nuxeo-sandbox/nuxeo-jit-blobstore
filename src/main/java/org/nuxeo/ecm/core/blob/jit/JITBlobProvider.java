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
package org.nuxeo.ecm.core.blob.jit;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.CachingBlobStore;
import org.nuxeo.ecm.core.blob.KeyStrategy;


public class JITBlobProvider extends BlobStoreBlobProvider {

    private static final Logger log = LogManager.getLogger(JITBlobProvider.class);

    protected JITBlobStoreConfiguration config;

    @Override
    protected BlobStore getBlobStore(String blobProviderId, Map<String, String> properties) throws IOException {
        config = getConfiguration(properties);
        log.info("Registering S3 blob provider '" + blobProviderId);
        KeyStrategy keyStrategy = getKeyStrategy();

        // main S3 blob store wrapped in a caching store
        BlobStore store = new JITBlobStore("JIT", config, keyStrategy);
        boolean caching = !config.getBooleanProperty("test-nocaching"); // for tests
        if (caching) {
            store = new CachingBlobStore("Cache", store, config.cachingConfiguration);
        }
        return store;
    }

    protected JITBlobStoreConfiguration getConfiguration(Map<String, String> properties) throws IOException {
        return new JITBlobStoreConfiguration(properties);
    }

    protected String getContentDispositionHeader(Blob blob) {
        return RFC2231.encodeContentDisposition(blob.getFilename(), false, null);
    }

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String getDigestAlgorithm() {
		return null;
	}

}
