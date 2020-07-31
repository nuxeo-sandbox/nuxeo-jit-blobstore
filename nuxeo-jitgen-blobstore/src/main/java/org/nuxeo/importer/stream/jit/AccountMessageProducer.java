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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.pattern.producer.AbstractProducer;

public class AccountMessageProducer extends AbstractProducer<DocumentMessage> {

	private static final Log log = LogFactory.getLog(AccountMessageProducer.class);

	protected int idx = 0;

	protected String blobStore;
	
	protected String[] csv;
	
	protected Stack<DocumentMessage> batch;
	
	public AccountMessageProducer(int producerId, String blobStore, String[] csv) {
		super(producerId);
		this.csv = csv;
		this.blobStore = blobStore;
		batch = new Stack<>();
		log.info("CustomerMessageProducer created");
	}

	@Override
	public int getPartition(DocumentMessage message, int partitions) {
		return getProducerId() % partitions;
	}

	@Override
	public boolean hasNext() {
		if (batch.size()>0) {
			return true;
		}
		if (idx < csv.length) {
			return csv[idx]!=null;
		}
		return false;
	}

	@Override
	public DocumentMessage next() {
		
		if (batch.size()==0) {
			batch.push(createAccount(csv[idx], false));
			batch.push(createAccount(csv[idx], true));	
			idx++;				
		}
		return batch.pop();
	}

	protected DocumentMessage createAccount(String line, boolean folder) {

		String[] meta = line.split(",");
		
		HashMap<String, Serializable> props = new HashMap<>();

		
		String fullname = meta[3].trim();
		int idx = fullname.indexOf(" ");		
		props.put("customer:firstname", fullname.substring(0, idx).trim());
		props.put("customer:lastname", fullname.substring(idx).trim());
		
		Map<String, String> address = new HashMap<String, String>();
		address.put("city", meta[5].trim());
		address.put("street", meta[4].trim());		
		address.put("country", "US");
		address.put("state", USStateHelper.getStateCode(meta[6].trim()));

		props.put("customer:address", (Serializable) address);
		
		
		String accountNumber = meta[8].trim();
		String customerID = accountNumber.substring(0,19);
		String accountKey = accountNumber.substring(20);
		
		props.put("customer:number",customerID);
		props.put("account:number",accountNumber);
						
		String name = accountKey;
		String stateName = USStateHelper.toPath(meta[6].trim());
		if ("fl".equalsIgnoreCase(stateName)) {
			stateName="florida";
		}
		
		String type = "Account";
		String path = "/" + stateName + "/" + customerID ;
		if (folder) {
			// create the Account Folder
			//path = path + "/" + name;
			props.put("dc:title", accountNumber);
			
		} else {
			// create the Correspondence file
			path = path + "/" + accountKey;
			name= "letter";
			props.put("dc:title", meta[1].trim());			
			type = "Correspondence" + USStateHelper.getStateCode(meta[6].trim());		
		}
		DocumentMessage.Builder builder = DocumentMessage.builder(type, path, name).setProperties(props);

		if (!folder) {
			BlobInfo bi = new BlobInfo();
			bi.key = blobStore + ":" + meta[0].trim();
			bi.digest=meta[0].trim();
			bi.filename=meta[1].trim();
			bi.mimeType="application/vnd.openxmlformats-officedocument.wordprocessingml.document";
			bi.length=Long.parseLong(meta[2].trim());					
			builder.setBlobInfo(bi);		
		}
			
		DocumentMessage msg = builder.build();
		return msg;
	}

 }
