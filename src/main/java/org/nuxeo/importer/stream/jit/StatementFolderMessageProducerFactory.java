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

import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.pattern.producer.ProducerFactory;
import org.nuxeo.lib.stream.pattern.producer.ProducerIterator;

public class StatementFolderMessageProducerFactory implements ProducerFactory<DocumentMessage> {

	protected final int nbMonths;

	public StatementFolderMessageProducerFactory(int nbMonths) {
		this.nbMonths = nbMonths;
	}

	@Override
	public ProducerIterator<DocumentMessage> createProducer(int producerId) {
		return new StatementFolderMessageProducer(producerId, nbMonths);
	}

	protected String getGroupName(int producerId) {
		return "StatementFolderMessageProducer." + producerId;
	}

}
