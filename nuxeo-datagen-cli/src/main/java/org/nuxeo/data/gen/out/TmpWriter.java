package org.nuxeo.data.gen.out;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class TmpWriter implements BlobWriter {

	public static final String NAME= "tmp";
	
	@Override
	public void write(byte[] data, String digest) throws Exception {
		File tmp = File.createTempFile(digest, ".pdf");;
		Files.copy(new ByteArrayInputStream(data), tmp.toPath(),StandardCopyOption.REPLACE_EXISTING);	
	}
	
	@Override
	public void flush() {
		// NOP
	}
	
}
