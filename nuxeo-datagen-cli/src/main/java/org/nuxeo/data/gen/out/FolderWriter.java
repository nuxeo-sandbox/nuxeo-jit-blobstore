package org.nuxeo.data.gen.out;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


public class FolderWriter extends AbstractBlobWriter implements BlobWriter {

	public static final String NAME= "file:";
	
	protected File folder;
	
	public FolderWriter (String folder) {
		this.folder = new File(folder);		
	}	
	
	@Override
	public void write(byte[] data, String digest) throws Exception {
		Path path = Path.of(folder.getAbsolutePath(), digest + "." + getExtension()); 
		Files.copy(wrap(data), path,StandardCopyOption.REPLACE_EXISTING);		
	}
	
	@Override
	public void flush() {
		// NOP
	}
}
