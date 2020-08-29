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

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.nuxeo.data.gen.pdf.StatementMeta;

public class FolderDigestTreeWriter extends FolderDigestWriter {

	public static final String NAME = "fileDigestTree:";
	protected final int depth;

	public FolderDigestTreeWriter(String folder, int depth) {
		super(folder);
		this.depth=depth;
	}

	@Override
	public void write(byte[] data, StatementMeta meta) throws Exception {
		write(data, meta.getDigest());
	}
	
	@Override
	public void write(byte[] data, String fileName) throws Exception {
		
		String[] segments = new String[depth];
		
		File root = new File(folder.getAbsolutePath());
		
		for (int i = 0; i < depth; i++) {
			segments[i]=fileName.substring(2*i, 2*i+2);
			root = new File(root, segments[i]);
			if (!root.exists()) {
				root.mkdir();
			}
		}		
		
		Path path = FileSystems.getDefault().getPath(root.getAbsolutePath(), fileName);
		Files.copy(wrap(data), path, StandardCopyOption.REPLACE_EXISTING);
	}
}
