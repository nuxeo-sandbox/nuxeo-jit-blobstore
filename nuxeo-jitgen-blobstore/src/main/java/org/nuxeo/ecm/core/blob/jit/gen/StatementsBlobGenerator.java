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
package org.nuxeo.ecm.core.blob.jit.gen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.data.gen.meta.FormatUtils;
import org.nuxeo.data.gen.meta.RandomDataGenerator;
import org.nuxeo.data.gen.pdf.StatementMeta;
import org.nuxeo.data.gen.pdf.itext.ITextNXBankStatementGenerator;
import org.nuxeo.data.gen.pdf.itext.ITextNXBankTemplateCreator;
import org.nuxeo.data.gen.pdf.itext.ITextNXBankTemplateCreator2;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

public class StatementsBlobGenerator extends DefaultComponent implements InMemoryBlobGenerator {

	protected RandomDataGenerator rnd = null;
	protected ITextNXBankTemplateCreator templateGen = null;
	protected ITextNXBankStatementGenerator gen;

	protected List<String> keyNames = new ArrayList<String>();

	@Override
	public void start(ComponentContext context) {
		try {
			initGenerator();
		} catch (Exception e) {
			throw new NuxeoException("Unable to initialize Random Data Generator", e);
		}
	}

	public void initGenerator() throws Exception {

		rnd = new RandomDataGenerator(true, true);
		templateGen = new ITextNXBankTemplateCreator2();

		// init random data generator
		rnd.init();

		// Generate the template
		InputStream logo = StatementsBlobGenerator.class.getResourceAsStream("/NxBank3.png");
		templateGen.init(logo);

		ByteArrayOutputStream templateOut = new ByteArrayOutputStream();
		templateGen.generate(templateOut);
		byte[] templateData = templateOut.toByteArray();

		// Init PDF generator
		gen = new ITextNXBankStatementGenerator();
		gen.init(new ByteArrayInputStream(templateData), templateGen.getKeys());
		gen.computeDigest = false;

		for (String k : templateGen.getKeys()) {
			keyNames.add(clean(k));
		}
	}

	protected String clean(String key) {
		key = key.replace("#", "");
		key = key.replace("-", "");
		return key;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == InMemoryBlobGenerator.class) {
			return adapter.cast(this);
		}
		return null;
	}

	@Override
	public String computeKey(Long userSeed, Long operationSeed, Integer month) {
		return rnd.seeds2Id(userSeed, operationSeed, month);
	}

	protected BlobInfo computeBlobInfo(String prefix, String key, String[] meta) {
		BlobInfo bi = new BlobInfo();
		bi.key = prefix + ":" + key;
		bi.encoding = "";
		bi.mimeType = "application/pdf";
		StringBuilder sb = new StringBuilder();
		sb.append(meta[5].trim());
		sb.append("-");
		sb.append(meta[4].trim().replace(" ", "_"));
		sb.append(".pdf");
		bi.filename = sb.toString();
		return bi;
	}

	@Override
	public BlobInfo computeBlobInfo(String prefix, String key) {
		return computeBlobInfo(prefix, key, getMetaDataForBlobKey(key));
	}

	public String[] getMetaDataForBlobKey(String key) {
		return rnd.generate(key);
	}

	public StatementMeta getStream(String key, OutputStream out) throws Exception {
		String[] meta = getMetaDataForBlobKey(key);
		return gen.generate(out, meta);
	}

	protected Map<String, String> wrap(String[] meta) {
		Map<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < keyNames.size(); i++) {
			map.put(keyNames.get(i), meta[i]);
		}
		return map;
	}

	@Override
	public Map<String, String> getMetaDataKey(String key) {
		return wrap(getMetaDataForBlobKey(key));
	}

	@Override
	public DocInfo computeDocInfo(String prefix, Long userSeed, Long operationSeed, Integer month) {
		DocInfo di = new DocInfo();
		String key = computeKey(userSeed, operationSeed, month);
		String[] meta = getMetaDataForBlobKey(key);
		di.metaData = wrap(meta);
		di.blobInfo = computeBlobInfo(prefix, key, meta);
		return di;
	}

	public boolean readBlob(String key, Path dest) throws IOException {
		String[] meta = getMetaDataForBlobKey(key);
		OutputStream out = Files.newOutputStream(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		try {
			gen.generate(out, meta);
			return true;
		} catch (Exception e) {
			throw new IOException("Unable to generate statement", e);
		}
	}

	@Override
	public InputStream getStream(String key) throws IOException {
		String[] meta = getMetaDataForBlobKey(key);
		ByteArrayOutputStream out = new ByteArrayOutputStream(5 * 1024);
		try {
			StatementMeta smt = gen.generate(out, meta);
			return new ByteArrayInputStream(out.toByteArray());
		} catch (Exception e) {
			throw new IOException("Unable to generate statement", e);
		}
	}

	public List<NodeInfo> getTimeHierarchy(int months, boolean childrenOnly) {
		
		List<NodeInfo> hierarchy = new ArrayList<NodeInfo>();
		List<NodeInfo> level2 = new ArrayList<NodeInfo>();
		
		SimpleDateFormat yf = new SimpleDateFormat("YYYY");
		SimpleDateFormat mf = new SimpleDateFormat("MMMM-YYYY");
		
		for (int m = 0; m < months; m++) {
			Date d = FormatUtils.getDateWithOffset(m);
			
			NodeInfo yi = new NodeInfo();
			yi.name = yf.format(d);
			NodeInfo mi = new NodeInfo();
			mi.name = mf.format(d);
			mi.parentPath=yi.getPath();
			
			if (!hierarchy.contains(yi)) {
				hierarchy.add(yi);
			}
			level2.add(mi);			
		}
		
		if (childrenOnly) {
			return level2;
		}
		hierarchy.addAll(level2);
		
		return hierarchy;
	}
}
