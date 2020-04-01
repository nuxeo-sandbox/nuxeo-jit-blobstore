package org.nuxeo.ecm.core.blob.jit.gen;

public class NodeInfo {

	public String name;
	
	public String parentPath;
	
	public String title;

	@Override
	public boolean equals(Object other) {
		if (other instanceof NodeInfo) {
			return other.toString().equals(this.toString());			
		};
		return false;
	}

	public String getPath() {
		if (parentPath!=null) {
			return parentPath +"/" + name;
		}
		return "/" + name;		
	}
	
	@Override
	public String toString() {
		return getPath();
	}	
	
	public String getTitle() {
		if (title==null) {
			return name;
		}
		return title;
	}
	
	public String getName() {
		return name;
	}
	
	public String getParentPath () {
		if (parentPath==null) {
			return "/";
		}		
		return parentPath;
	}
	
	
}
