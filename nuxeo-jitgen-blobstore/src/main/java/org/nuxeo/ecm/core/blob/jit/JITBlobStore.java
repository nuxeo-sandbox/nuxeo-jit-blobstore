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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.blob.AbstractBlobStore;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobWriteContext;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.runtime.api.Framework;

public class JITBlobStore extends AbstractBlobStore {

	private static final Logger log = LogManager.getLogger(JITBlobStore.class);

	protected final JITBlobStoreConfiguration config;

	public JITBlobStore(String name, JITBlobStoreConfiguration config, KeyStrategy keyStrategy) {
		super(name, keyStrategy);
		this.config = config;
	}

	@Override
	public OptionalOrUnknown<Path> getFile(String key) {
        try {
            Path tmp = Files.createTempFile("tmp_", ".tmp");
            Framework.trackFile(tmp.toFile(), tmp);
            readBlob(key, tmp);
            return OptionalOrUnknown.of(tmp);
        } catch (IOException e) {
            throw new UnsupportedOperationException();
        }
	}

	@Override
	public OptionalOrUnknown<InputStream> getStream(String key) throws IOException {
		return OptionalOrUnknown.of(Framework.getService(InMemoryBlobGenerator.class).getStream(key));
	}

	@Override
	public boolean readBlob(String key, Path dest) throws IOException {		
		return Framework.getService(InMemoryBlobGenerator.class).readBlob(key, dest);
	}

	/************* WRITE OPS NOT SUPPORTED *****************/

	@Override
	public BinaryGarbageCollector getBinaryGarbageCollector() {
		return null;
	}

	@Override
	public String writeBlob(BlobWriteContext arg0) throws IOException {
		throw new IOException("ReadOnly BlobStore");
	}

	@Override
	public void deleteBlob(String arg0) {
		// NOP
	}

	@Override
	public boolean copyBlob(String arg0, BlobStore arg1, String arg2, boolean arg3) throws IOException {
		throw new IOException("ReadOnly BlobStore");
	}

}
