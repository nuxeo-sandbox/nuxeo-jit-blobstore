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
package org.nuxeo.data.gen.out;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.nuxeo.data.gen.pdf.StatementMeta;

public abstract class AbstractBlobWriter implements BlobWriter {

	protected InputStream wrap(byte[] pdf) throws Exception {
		return new ByteArrayInputStream(pdf);
	}

	public void write(byte[] data, StatementMeta meta) throws Exception {
		write(data, meta.getFileName());
	}

	public void write(byte[] data, String fileNane) throws Exception {
		throw new UnsupportedOperationException();
	}

}
