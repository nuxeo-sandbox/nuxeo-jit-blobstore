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

import java.util.List;

import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.pattern.producer.ProducerFactory;
import org.nuxeo.lib.stream.pattern.producer.ProducerIterator;

public class CustomerMessageProducerFactoryMT extends BaseMessageProducerFactory implements ProducerFactory<DocumentMessage> {

	protected List<List<String>> csvs;
	
	public CustomerMessageProducerFactoryMT(List<List<String>> csvs) {	
		this.csvs = csvs;
	}

	@Override
	public ProducerIterator<DocumentMessage> createProducer(int producerId) {
		List<String> csv = csvs.get(producerId);
		return new CustomerMessageProducer(producerId, SNOWBALL, csv.toArray(new String[0]));
	}

	protected String getGroupName(int producerId) {
		return "CustomerMessageProducerFactory." + producerId;
	}

}
