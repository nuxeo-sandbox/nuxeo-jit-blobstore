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

import java.util.Random;

import org.nuxeo.data.gen.meta.SequenceGenerator;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.pattern.Message;
import org.nuxeo.lib.stream.pattern.producer.ProducerFactory;
import org.nuxeo.lib.stream.pattern.producer.ProducerIterator;

public class StatementDocumentMessageProducerFactory implements ProducerFactory<Message> {

	protected final long nbDocuments;
	protected final int nbMonth;
	protected final int monthOffset;
	protected SequenceGenerator sequenceGen;   
	protected final String batchTag;
	protected boolean useRecords;
	/**
	 * Generates random documents messages that point to existing blobs.
	 */
	public StatementDocumentMessageProducerFactory(Long seed, long skip, long nbDocuments,int nbMonth, int monthOffset, String batchTag, boolean useRecords) {
		this.nbDocuments = nbDocuments;
		this.nbMonth=nbMonth;
		this.monthOffset=monthOffset;
		sequenceGen = new SequenceGenerator(seed, nbMonth);	
		sequenceGen.setMonthOffset(monthOffset);
		if (skip>0) {
			sequenceGen.skip(skip);
		}
		if (batchTag!=null) {
			this.batchTag=batchTag;	
		} else {
			this.batchTag="B" + new Random().nextInt();
		}
		this.useRecords=useRecords;
	}

	@Override
	public ProducerIterator<Message> createProducer(int producerId) {
		return new StatementDocumentMessageProducer(sequenceGen, producerId, nbDocuments, nbMonth, monthOffset, batchTag, useRecords);
	}

	protected String getGroupName(int producerId) {
		return "StatementDocumentMessageProducer." + producerId;
	}

}
