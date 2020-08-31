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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.data.gen.meta.RandomDataGenerator;
import org.nuxeo.data.gen.meta.SequenceGenerator;
import org.nuxeo.ecm.core.blob.jit.gen.DocInfo;
import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.ecm.core.blob.jit.gen.NodeInfo;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.pattern.Message;
import org.nuxeo.lib.stream.pattern.producer.AbstractProducer;
import org.nuxeo.runtime.api.Framework;

public class StatementDocumentMessageProducer<M extends Message> extends AbstractProducer<M> {
	private static final Log log = LogFactory.getLog(StatementDocumentMessageProducer.class);

	protected final long nbDocuments;

	protected int documentCount = 0;

	protected final List<NodeInfo> hierarchy;
	
	protected final SequenceGenerator sequenceGen;

	protected final String batchTag;
	
	protected final boolean useRecords;

	protected final boolean withStates;
	
	protected final boolean storeInCustomerFolder;	
	
	protected final boolean storeInRoot;
	
	protected final int nbMonths;
	
	public StatementDocumentMessageProducer(SequenceGenerator sequenceGen, int producerId, long nbDocuments, int nbMonths, int monthOffset, String batchTag, boolean useRecords, boolean withStates, boolean storeInCustomerFolder, boolean storeInRoot) {
		super(producerId);
		this.nbDocuments = nbDocuments;
		this.nbMonths=nbMonths;
		this.withStates=withStates;
		if (withStates) {
			hierarchy = HierarchyHelper.generateStateYearMonthHierarchy(nbMonths+monthOffset);
		} else {
			hierarchy = HierarchyHelper.generateYearMonthHierarchy(nbMonths+monthOffset);
		}
		this.sequenceGen=sequenceGen;
		this.batchTag=batchTag;
		this.useRecords=useRecords;
		this.storeInCustomerFolder=storeInCustomerFolder;
		this.storeInRoot=storeInRoot;
		log.info("StatementDocumentMessageProducer created, nbDocuments: " + nbDocuments);
	}

	protected InMemoryBlobGenerator getGen() {
		return Framework.getService(InMemoryBlobGenerator.class);
	}

	@Override
	public int getPartition(Message message, int partitions) {
		return getProducerId() % partitions;
	}

	@Override
	public boolean hasNext() {
		return documentCount < nbDocuments;
	}

	
	@Override
	public M next() {
		DocumentMessage ret;

		try {
			SequenceGenerator.Entry entry = sequenceGen.next();
			ret = createDocument(entry);			
			if (useRecords) {
				return (M)new RecordDocumentMessage(ret);
			}
			return (M)ret;
		} catch (Exception e) {
			log.error("Unable to generate message", e);
			throw e;
		}
	}

	protected DocumentMessage createDocument(SequenceGenerator.Entry entry) {

		String parentPath;
		long currentAccountSeed = entry.getAccountKeyLong();
		long currentDataSeed = entry.getDataKey();
		int month = entry.getMonth();
		
		DocInfo docInfo = getGen().computeDocInfo("jit", currentAccountSeed, currentDataSeed, month);

		if (storeInCustomerFolder) {
			String accountID = docInfo.getMeta("ACCOUNTID").trim();
			String customerId = accountID.substring(0,19);
			String state = docInfo.getMeta("STATE").trim();
			String stateName = USStateHelper.toPath(state);		
			if ("fl".equalsIgnoreCase(stateName)) {
				stateName="florida";
			}
			parentPath = "/" + stateName + "/" + customerId + "/" + accountID.substring(20);
		} else 	if (withStates) {			
			String state = docInfo.getMeta("STATE");
			int stateOffset = USStateHelper.getOffset(state);
			if (stateOffset==-1) {
				log.error("Unable find mapping for state " + state + " using Alabama");
				stateOffset=0;
			}
			int nbYears = nbMonths/12;			
			int idx = stateOffset*(nbYears+nbMonths+1) + nbYears + entry.getMonth() +1 ;
			parentPath = hierarchy.get(idx).getPath();
		} else if (storeInRoot) {
			parentPath = "/";
		} else {
			int nbYears = nbMonths/12;
			parentPath = hierarchy.get(nbYears+entry.getMonth()).getPath();
		}
		
		String title = getTitle(docInfo);
		String name = getName(title);

		HashMap<String, Serializable> props = new HashMap<>();
		props.put("dc:source", "initialImport");
		props.put("dc:title", title);
		if (batchTag!=null) {
			props.put("dc:publisher", batchTag);
		}
		mapMetaData(props, docInfo);
		
		DocumentMessage.Builder builder = DocumentMessage.builder("Statement", parentPath, name).setProperties(props);
		
		// blobInfo.length can not be null and we do not yet know the actual size
		docInfo.blobInfo.length=-1L;
		builder.setBlobInfo(docInfo.blobInfo);

		DocumentMessage node = builder.build();
		documentCount++;
		return node;
	}

	protected void mapMetaData(HashMap<String, Serializable> props, DocInfo docInfo) {
		props.put("account:number", docInfo.getMeta("ACCOUNTID").trim());
		try {
			Date stmDate = RandomDataGenerator.df.get().parse(docInfo.getMeta("DATE").trim());
			props.put("all:documentDate", stmDate);
			props.put("dc:created", stmDate);
			props.put("dc:modified", stmDate);
			props.put("dc:creator", "nco-admin");						
		} catch (Exception e) {
			log.error("Unable to parse date", e);
		}		
		String fullname = docInfo.getMeta("NAME").trim();
		int idx = fullname.indexOf(" ");		
		props.put("customer:firstname", fullname.substring(0, idx).trim());
		props.put("customer:lastname", fullname.substring(idx).trim());
		
		Map<String, String> address = new HashMap<String, String>();
		address.put("city", docInfo.getMeta("CITY").trim());
		address.put("street", docInfo.getMeta("STREET").trim());
		address.put("country", "US");
		address.put("state", USStateHelper.getStateCode(docInfo.getMeta("STATE").trim()));
		props.put("customer:address", (Serializable) address);

		props.put("customer:number", docInfo.getMeta("ACCOUNTID").trim().substring(0,19));
	}

	protected String getName(String title) {
		return title.replaceAll("\\W+", "-").toLowerCase();
	}

	protected String getTitle(DocInfo docInfo) {
		return docInfo.getFileName();
	}

}
