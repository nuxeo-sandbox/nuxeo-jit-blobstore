package org.nuxeo.data.gen.out;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class TmpWriter extends AbstractBlobWriter implements BlobWriter {

	public static final String NAME= "tmp";
	
	@Override
	public void write(byte[] data, String digest) throws Exception {
		File tmp = File.createTempFile(digest, "." + getExtension());;
		Files.copy(wrap(data), tmp.toPath(),StandardCopyOption.REPLACE_EXISTING);	
	}
	
	@Override
	public void flush() {
		// NOP
	}
	
}
