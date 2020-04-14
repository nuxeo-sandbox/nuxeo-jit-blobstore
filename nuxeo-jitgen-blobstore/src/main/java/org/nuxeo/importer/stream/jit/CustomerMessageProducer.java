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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.pattern.producer.AbstractProducer;

public class CustomerMessageProducer extends AbstractProducer<DocumentMessage> {

	private static final Log log = LogFactory.getLog(CustomerMessageProducer.class);

	protected int idx = 0;

	protected String[] csv;
	
	public CustomerMessageProducer(int producerId, String[] csv) {
		super(producerId);
		this.csv = csv;
		log.info("CustomerMessageProducer created");
	}

	@Override
	public int getPartition(DocumentMessage message, int partitions) {
		return getProducerId() % partitions;
	}

	@Override
	public boolean hasNext() {
		if (idx < csv.length) {
			return csv[idx]!=null;
		}
		return false;
	}

	@Override
	public DocumentMessage next() {
		DocumentMessage ret = createCustomer(csv[idx]);
		idx++;
		return ret;
	}

	protected DocumentMessage createCustomer(String line) {

		String[] meta = line.split(",");
		
		HashMap<String, Serializable> props = new HashMap<>();
		props.put("dc:title", meta[3].trim());

		
		String fullname = meta[3].trim();
		int idx = fullname.indexOf(" ");		
		props.put("all:customerFirstname", fullname.substring(0, idx).trim());
		props.put("all:customerLastname", fullname.substring(idx).trim());
		
		Map<String, String> address = new HashMap<String, String>();
		address.put("city", meta[5].trim());
		address.put("street", meta[4].trim());		
		props.put("all:customerAddress", (Serializable) address);		
						
		props.put("all:customerNumber",meta[8].trim().substring(0,19));
						
		String name = meta[8].trim().substring(0,19);
		String stateName = USStateHelper.toPath(meta[6].trim());
		
		DocumentMessage.Builder builder = DocumentMessage.builder("CustomerDocument", "/" +stateName, name).setProperties(props);

		BlobInfo bi = new BlobInfo();
		bi.key = "test:" + meta[0].trim();
		bi.digest=meta[0].trim();
		bi.filename=meta[1].trim();
		bi.mimeType="image/jpeg";
		bi.length=Long.parseLong(meta[2].trim());		
		
		builder.setBlobInfo(bi);		
		
		DocumentMessage msg = builder.build();
		return msg;
	}

}
