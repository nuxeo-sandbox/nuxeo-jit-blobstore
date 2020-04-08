package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.blob.jit.gen.DocInfo;
import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.ecm.core.blob.jit.gen.NodeInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
public class TestDataGenService {

	@Test
	public void canAccessService() {

		InMemoryBlobGenerator imbg = Framework.getService(InMemoryBlobGenerator.class);
		assertNotNull(imbg);

		String key = imbg.computeKey(1L, 1L, 1);

		Map<String, String> meta = imbg.getMetaDataKey(key);
		assertNotNull(meta);
		assertTrue(meta.size() > 0);

	}
	
	@Test
	public void  testTimeHierarchy() {
		
		InMemoryBlobGenerator imbg = Framework.getService(InMemoryBlobGenerator.class);
		assertNotNull(imbg);
		
		List<NodeInfo> nodes = imbg.getTimeHierarchy(48, false);		
		assertEquals(48+4, nodes.size());
		for (int i = 0; i < 48; i++) {
			assertEquals(nodes.get(i/12).getPath(), nodes.get(4+i).parentPath);
		}
	}
	
	@Test
	public void  dumpMetaData() {
		
		InMemoryBlobGenerator imbg = Framework.getService(InMemoryBlobGenerator.class);
		assertNotNull(imbg);
		
		DocInfo di = imbg.computeDocInfo("test", 1L, 1L, 48);
		assertNotNull(di);
		
		List<String> keys = new ArrayList<String>();
		keys.addAll(di.getMetaDataKeys());		
		Collections.sort(keys);		
		
		for (String key:keys) {
			System.out.println(key + " : " + di.getMeta(key));
		}
		
	}
	

}
