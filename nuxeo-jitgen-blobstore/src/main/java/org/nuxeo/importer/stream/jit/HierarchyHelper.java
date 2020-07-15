package org.nuxeo.importer.stream.jit;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.ecm.core.blob.jit.gen.NodeInfo;
import org.nuxeo.runtime.api.Framework;

public class HierarchyHelper {

	public static List<NodeInfo> generateYearMonthHierarchy(int nbMonths) {
		InMemoryBlobGenerator gen = Framework.getService(InMemoryBlobGenerator.class);
		return gen.getTimeHierarchy(nbMonths, false);
	}
	
	public static List<NodeInfo> generateStateYearMonthHierarchy(int nbMonths) {
		
		List<NodeInfo> nodes = new ArrayList<NodeInfo>();
		
		for (String stateName : USStateHelper.STATES) {
			
			NodeInfo sNode = new NodeInfo();
			sNode.name = USStateHelper.toPath(stateName);
			sNode.title = stateName;
			
			nodes.add(sNode);
			
			List<NodeInfo> months = generateYearMonthHierarchy(nbMonths);
			for (NodeInfo month: months) {
				if (month.parentPath==null) {
					month.parentPath = sNode.getPath();	
				} else {
					month.parentPath =sNode.getPath() + month.getParentPath() ;
				}
				nodes.add(month);
			}			
		}
		return nodes;
	}
	
}
