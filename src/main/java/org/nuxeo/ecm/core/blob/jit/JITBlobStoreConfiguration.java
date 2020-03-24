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

import org.nuxeo.ecm.core.blob.AbstractBlobStoreConfiguration;
import org.nuxeo.ecm.core.blob.CachingConfiguration;


public class JITBlobStoreConfiguration extends AbstractBlobStoreConfiguration {

    public static final String SYSTEM_PROPERTY_PREFIX = "nuxeo.jit.blobstore";

    public final CachingConfiguration cachingConfiguration;

	public JITBlobStoreConfiguration(Map<String, String> properties) throws IOException {
		super(SYSTEM_PROPERTY_PREFIX, properties);
		cachingConfiguration = new CachingConfiguration(SYSTEM_PROPERTY_PREFIX, properties);
	}

}
