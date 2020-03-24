package org.nuxeo.ecm.core.blob.jit.gen;

public class SmtMeta {

	protected String digest;

	protected  String[] keys;

	protected String fileName;
	
	protected long fileSize;
	
	public SmtMeta(String digest, String fileName, long fileSize, String[] keys) {
		super();
		this.digest = digest;
		this.keys = keys;
		this.fileName=fileName;
		this.fileSize=fileSize;
	}

	public String getDigest() {
		return digest;
	}

	public String[] getKeys() {
		return keys;
	}

	public String getFileName() {
		return fileName;
	}

	public long getFileSize() {
		return fileSize;
	}

}
