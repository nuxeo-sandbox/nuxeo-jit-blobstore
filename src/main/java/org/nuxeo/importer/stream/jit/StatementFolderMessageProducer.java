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

package org.nuxeo.importer.stream.jit;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.ecm.core.blob.jit.gen.NodeInfo;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.pattern.producer.AbstractProducer;
import org.nuxeo.runtime.api.Framework;

public class StatementFolderMessageProducer extends AbstractProducer<DocumentMessage> {
	private static final Log log = LogFactory.getLog(StatementFolderMessageProducer.class);

	protected final long nbMonths;

	protected int folderIndx = 0;

	protected static final int NB_STATEMENTS = 24;

	public static final String FOLDER_DESC_PREFIX = "Folder Containing Statements";

	protected final List<NodeInfo> nodes;

	public StatementFolderMessageProducer(int producerId, int nbMonths) {
		super(producerId);
		this.nbMonths = nbMonths;
		InMemoryBlobGenerator gen = Framework.getService(InMemoryBlobGenerator.class);
		nodes = gen.getTimeHierarchy(nbMonths);
		log.info("StatementFolderMessageProducer created, nbMonths: " + nbMonths);
	}

	@Override
	public int getPartition(DocumentMessage message, int partitions) {
		return getProducerId() % partitions;
	}

	@Override
	public boolean hasNext() {
		return nodes.size() > 0;
	}

	@Override
	public DocumentMessage next() {
		DocumentMessage ret;

		NodeInfo node = nodes.remove(0);

		ret = createFolder(node);

		return ret;
	}

	protected DocumentMessage createFolder(NodeInfo node) {

		HashMap<String, Serializable> props = new HashMap<>();
		props.put("dc:title", node.getTitle());
		props.put("dc:description", FOLDER_DESC_PREFIX);

		DocumentMessage.Builder builder = DocumentMessage.builder("Folder", node.getParentPath(), node.getName())
				.setProperties(props);

		DocumentMessage msg = builder.build();
		return msg;
	}

}
