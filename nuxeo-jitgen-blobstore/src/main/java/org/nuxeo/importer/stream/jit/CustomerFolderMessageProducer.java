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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.pattern.producer.AbstractProducer;

public class CustomerFolderMessageProducer extends AbstractProducer<DocumentMessage> {

	private static final Log log = LogFactory.getLog(CustomerFolderMessageProducer.class);

	protected int stateIdx = 0;

	public CustomerFolderMessageProducer(int producerId) {
		super(producerId);
		log.info("CustomerFolderMessageProducer created");
	}

	@Override
	public int getPartition(DocumentMessage message, int partitions) {
		return getProducerId() % partitions;
	}

	@Override
	public boolean hasNext() {
		return stateIdx < USStateHelper.STATES.length ;
	}

	@Override
	public DocumentMessage next() {
		DocumentMessage ret = createFolder(USStateHelper.STATES[stateIdx]);
		stateIdx++;
		return ret;
	}

	protected DocumentMessage createFolder(String state) {

		HashMap<String, Serializable> props = new HashMap<>();
		props.put("dc:title", state);

		String name = USStateHelper.toPath(state);
		DocumentMessage.Builder builder = DocumentMessage.builder("Folder", "/", name).setProperties(props);

		DocumentMessage msg = builder.build();
		return msg;
	}

}
